package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak;

import com.fedebacelar.bank.onboarding.application.model.CredentialSetupState;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialIdentityConflictException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakRoleResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserResponse;
import feign.FeignException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KeycloakCredentialProvisioningAdapter implements CredentialProvisioningPort {
    private static final List<String> REQUIRED_ACTIONS = List.of("UPDATE_PROFILE", "UPDATE_PASSWORD");
    private static final List<String> CUSTOMER_ROLES = List.of("CUSTOMER_READ", "ACCOUNT_READ", "IDENTITY_READ");
    private final KeycloakAdminFeignClient client;
    private final String realm;
    private final String actionClientId;
    private final String redirectUri;
    private final int lifespanSeconds;

    public KeycloakCredentialProvisioningAdapter(
            KeycloakAdminFeignClient client,
            @Value("${onboarding.keycloak.realm:banking-ecosystem}") String realm,
            @Value("${onboarding.keycloak.action-client-id:home-banking-bff}") String actionClientId,
            @Value("${onboarding.keycloak.credential-redirect-uri:http://localhost:4200/onboarding/credentials-complete}") String redirectUri,
            @Value("${onboarding.keycloak.action-lifespan:PT24H}") Duration lifespan
    ) {
        this.client = client;
        this.realm = realm;
        this.actionClientId = actionClientId;
        this.redirectUri = redirectUri;
        this.lifespanSeconds = Math.toIntExact(lifespan.toSeconds());
    }

    @Override
    public String precreateUser(UUID applicationId, String email, ApplicantData applicantData) {
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
        client.executeActionsEmail(realm, userId, actionClientId, redirectUri, lifespanSeconds, REQUIRED_ACTIONS);
    }

    @Override
    public CredentialSetupState getCredentialSetupState(String userId) {
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
}
