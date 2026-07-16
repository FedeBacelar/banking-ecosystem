package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak;

import com.fedebacelar.bank.onboarding.application.model.CredentialSetupState;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialIdentityConflictException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakRoleResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserResponse;
import com.fedebacelar.bank.onboarding.infrastructure.validation.OutboundLinkUriValidator;
import feign.FeignException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KeycloakCredentialProvisioningAdapter implements CredentialProvisioningPort {
    private static final String INSTRUMENTATION_SCOPE = "com.fedebacelar.bank.onboarding.keycloak";
    private static final List<String> REQUIRED_ACTIONS = List.of("UPDATE_PROFILE", "UPDATE_PASSWORD");
    private static final List<String> CUSTOMER_ROLES = List.of("HOME_BANKING_USER");
    private static final String CREDENTIAL_COMPLETION_PATH = "/web/auth/login/onboarding-completion";
    private final KeycloakAdminFeignClient client;
    private final String realm;
    private final String actionClientId;
    private final String redirectUri;
    private final int lifespanSeconds;
    private final Tracer tracer;

    public KeycloakCredentialProvisioningAdapter(
            KeycloakAdminFeignClient client,
            @Value("${onboarding.keycloak.realm:banking-ecosystem}") String realm,
            @Value("${onboarding.keycloak.action-client-id:home-banking-bff}") String actionClientId,
            @Value("${onboarding.keycloak.credential-redirect-uri:http://localhost:8085/web/auth/login/onboarding-completion}") String redirectUri,
            @Value("${onboarding.keycloak.action-lifespan:PT24H}") Duration lifespan,
            OpenTelemetry openTelemetry
    ) {
        this.client = client;
        this.realm = realm;
        this.actionClientId = actionClientId;
        this.redirectUri = OutboundLinkUriValidator.validate(
                redirectUri,
                "onboarding.keycloak.credential-redirect-uri",
                CREDENTIAL_COMPLETION_PATH
        ).toASCIIString();
        this.lifespanSeconds = Math.toIntExact(lifespan.toSeconds());
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE);
    }

    @Override
    public String precreateUser(UUID applicationId, String email, ApplicantData applicantData) {
        return traceKeycloak("precreate_user", () -> precreateUserInternal(applicationId, email, applicantData));
    }

    private String precreateUserInternal(UUID applicationId, String email, ApplicantData applicantData) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String provisionalUsername = "pending-" + applicationId.toString().replace("-", "");
        try {
            ResponseEntity<Void> response = client.createUser(realm, new KeycloakUserRequest(
                    provisionalUsername, normalizedEmail, applicantData.firstName(), applicantData.lastName(),
                    true, true, REQUIRED_ACTIONS
            ));
            String userId = userIdFromLocation(response.getHeaders().getLocation());
            assignCustomerRoles(userId);
            return userId;
        } catch (FeignException.Conflict conflict) {
            List<KeycloakUserResponse> users = client.findUsers(realm, normalizedEmail, true);
            if (users.size() != 1 || !users.getFirst().username().equals(provisionalUsername)) {
                throw new CredentialIdentityConflictException();
            }
            String userId = users.getFirst().id();
            assignCustomerRoles(userId);
            return userId;
        }
    }

    @Override
    public void sendCredentialSetupEmail(String userId) {
        traceKeycloak("send_actions_email", () -> {
            client.executeActionsEmail(realm, userId, actionClientId, redirectUri, lifespanSeconds, REQUIRED_ACTIONS);
            return null;
        });
    }

    @Override
    public CredentialSetupState getCredentialSetupState(String userId) {
        return traceKeycloak("check_credential_state", () -> getCredentialSetupStateInternal(userId));
    }

    private CredentialSetupState getCredentialSetupStateInternal(String userId) {
        KeycloakUserResponse user = client.getUser(realm, userId);
        boolean hasPassword = client.getCredentials(realm, userId).stream().anyMatch(credential -> "password".equals(credential.type()));
        boolean actionsComplete = user.requiredActions() == null || user.requiredActions().stream().noneMatch(REQUIRED_ACTIONS::contains);
        boolean usernameChosen = user.username() != null && !user.username().startsWith("pending-");
        return new CredentialSetupState(user.enabled() && hasPassword && actionsComplete && usernameChosen, user.username());
    }

    private void assignCustomerRoles(String userId) {
        List<KeycloakRoleResponse> roles = CUSTOMER_ROLES.stream().map(role -> client.getRole(realm, role)).toList();
        client.assignRealmRoles(realm, userId, roles);
    }

    private String userIdFromLocation(URI location) {
        if (location == null || location.getPath() == null || !location.getPath().contains("/")) {
            throw new IllegalStateException("Keycloak did not return the created user location.");
        }
        return location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
    }

    private <T> T traceKeycloak(String operation, Supplier<T> execution) {
        Span span;
        try {
            span = tracer.spanBuilder("keycloak.admin." + operation)
                    .setSpanKind(SpanKind.CLIENT)
                    .setAttribute("peer.service", "keycloak")
                    .setAttribute("nerva.keycloak.operation", operation)
                    .startSpan();
        } catch (RuntimeException telemetryFailure) {
            return execution.get();
        }

        Scope scope;
        try {
            scope = span.makeCurrent();
        } catch (RuntimeException telemetryFailure) {
            safeEnd(span);
            return execution.get();
        }

        try {
            return execution.get();
        } catch (RuntimeException | Error executionFailure) {
            safeFailure(span, executionFailure);
            throw executionFailure;
        } finally {
            safeClose(scope);
            safeEnd(span);
        }
    }

    private void safeFailure(Span span, Throwable failure) {
        try {
            span.setStatus(StatusCode.ERROR);
            span.setAttribute("error.type", failure.getClass().getSimpleName());
        } catch (RuntimeException ignored) {
            // Identity-provider behavior must never depend on telemetry state.
        }
    }

    private void safeClose(Scope scope) {
        try {
            scope.close();
        } catch (RuntimeException ignored) {
            // Telemetry cleanup is fail-open.
        }
    }

    private void safeEnd(Span span) {
        try {
            span.end();
        } catch (RuntimeException ignored) {
            // Telemetry cleanup is fail-open.
        }
    }
}
