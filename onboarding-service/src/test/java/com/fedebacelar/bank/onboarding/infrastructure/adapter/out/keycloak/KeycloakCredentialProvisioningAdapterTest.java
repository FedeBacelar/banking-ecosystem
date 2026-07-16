package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakCredentialResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakRoleResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserResponse;
import feign.FeignException;
import feign.Request;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KeycloakCredentialProvisioningAdapterTest {

    private SdkTracerProvider tracerProvider;

    @AfterEach
    void tearDown() {
        if (tracerProvider != null) {
            tracerProvider.close();
        }
    }

    @Test
    void reconcilesRealmRolesWhenCreateResponseWasLostAfterUserCreation() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        UUID applicationId = UUID.randomUUID();
        String username = "pending-" + applicationId.toString().replace("-", "");
        FeignException.Conflict conflict = new FeignException.Conflict(
                "Conflict", Request.create(Request.HttpMethod.POST, "http://keycloak/admin/users",
                Map.of(), null, StandardCharsets.UTF_8, null), new byte[0], Map.of()
        );
        when(client.createUser(anyString(), any(KeycloakUserRequest.class))).thenThrow(conflict);
        when(client.findUsers("banking-ecosystem", "person@example.com", true)).thenReturn(List.of(
                new KeycloakUserResponse("keycloak-user-id", username, "person@example.com", true,
                        List.of("UPDATE_PROFILE", "UPDATE_PASSWORD"))
        ));
        when(client.getRole(anyString(), anyString())).thenAnswer(invocation ->
                new KeycloakRoleResponse(UUID.randomUUID().toString(), invocation.getArgument(1)));
        KeycloakCredentialProvisioningAdapter adapter = adapter(client);

        String userId = adapter.precreateUser(applicationId, "Person@Example.com", applicant(applicationId));

        assertThat(userId).isEqualTo("keycloak-user-id");
        verify(client).assignRealmRoles(eq("banking-ecosystem"), eq("keycloak-user-id"),
                org.mockito.ArgumentMatchers.argThat(roles -> roles.stream().map(KeycloakRoleResponse::name).toList()
                        .equals(List.of("HOME_BANKING_USER"))));
    }

    @Test
    void considersCredentialsCompleteOnlyAfterUsernamePasswordAndActionsAreResolved() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        when(client.getUser("banking-ecosystem", "keycloak-user-id")).thenReturn(
                new KeycloakUserResponse("keycloak-user-id", "federico.bacelar", "person@example.com", true, List.of())
        );
        when(client.getCredentials("banking-ecosystem", "keycloak-user-id"))
                .thenReturn(List.of(new KeycloakCredentialResponse("credential-id", "password")));

        var state = adapter(client).getCredentialSetupState("keycloak-user-id");

        assertThat(state.complete()).isTrue();
        assertThat(state.username()).isEqualTo("federico.bacelar");
    }

    @Test
    void sendsCredentialActionsBackThroughTheBffCompletionEntryPoint() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);

        adapter(client).sendCredentialSetupEmail("keycloak-user-id");

        verify(client).executeActionsEmail(
                "banking-ecosystem",
                "keycloak-user-id",
                "home-banking-bff",
                "http://localhost:8085/web/auth/login/onboarding-completion",
                Math.toIntExact(Duration.ofHours(24).toSeconds()),
                List.of("UPDATE_PROFILE", "UPDATE_PASSWORD")
        );
    }

    @Test
    void representsKeycloakAsAClientDependencyWithoutIdentityData() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        InMemorySpanExporter exporter = InMemorySpanExporter.create();

        adapter(
                client,
                "http://localhost:8085/web/auth/login/onboarding-completion",
                openTelemetry(exporter)
        ).sendCredentialSetupEmail("sensitive-keycloak-user-id");

        var span = exporter.getFinishedSpanItems().getFirst();
        assertThat(span.getName()).isEqualTo("keycloak.admin.send_actions_email");
        assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);
        assertThat(span.getAttributes().asMap()).containsOnly(
                java.util.Map.entry(
                        io.opentelemetry.api.common.AttributeKey.stringKey("peer.service"),
                        "keycloak"
                ),
                java.util.Map.entry(
                        io.opentelemetry.api.common.AttributeKey.stringKey("nerva.keycloak.operation"),
                        "send_actions_email"
                )
        );
        assertThat(span.toString())
                .doesNotContain("sensitive-keycloak-user-id")
                .doesNotContain("banking-ecosystem")
                .doesNotContain("onboarding-completion");
    }

    @Test
    void telemetryFailureDoesNotPreventTheKeycloakRequest() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        Tracer tracer = mock(Tracer.class);
        when(openTelemetry.getTracer(anyString())).thenReturn(tracer);
        when(tracer.spanBuilder(anyString())).thenThrow(new IllegalStateException("collector unavailable"));

        adapter(
                client,
                "http://localhost:8085/web/auth/login/onboarding-completion",
                openTelemetry
        ).sendCredentialSetupEmail("keycloak-user-id");

        verify(client).executeActionsEmail(
                anyString(), anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), any()
        );
    }

    @Test
    void marksARealKeycloakFailureAndPropagatesItWithoutSensitiveDetails() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        IllegalStateException failure = new IllegalStateException("sensitive identity-provider detail");
        when(client.getUser("banking-ecosystem", "sensitive-keycloak-user-id")).thenThrow(failure);
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        KeycloakCredentialProvisioningAdapter adapter = adapter(
                client,
                "http://localhost:8085/web/auth/login/onboarding-completion",
                openTelemetry(exporter)
        );

        assertThatThrownBy(() -> adapter.getCredentialSetupState("sensitive-keycloak-user-id"))
                .isSameAs(failure);

        var span = exporter.getFinishedSpanItems().getFirst();
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getAttributes().asMap()).containsEntry(
                io.opentelemetry.api.common.AttributeKey.stringKey("error.type"),
                "IllegalStateException"
        );
        assertThat(span.toString())
                .doesNotContain("sensitive-keycloak-user-id")
                .doesNotContain("sensitive identity-provider detail");
    }

    @Test
    void acceptsAnHttpsCredentialCompletionRedirect() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        String redirectUri = "https://bank.nerva.example/web/auth/login/onboarding-completion";

        adapter(client, redirectUri).sendCredentialSetupEmail("keycloak-user-id");

        verify(client).executeActionsEmail(
                "banking-ecosystem",
                "keycloak-user-id",
                "home-banking-bff",
                redirectUri,
                Math.toIntExact(Duration.ofHours(24).toSeconds()),
                List.of("UPDATE_PROFILE", "UPDATE_PASSWORD")
        );
    }

    @Test
    void rejectsUnsafeOrNonCanonicalCredentialCompletionRedirects() {
        KeycloakAdminFeignClient client = mock(KeycloakAdminFeignClient.class);
        List<String> invalidRedirectUris = List.of(
                "/web/auth/login/onboarding-completion",
                "ftp://localhost:8085/web/auth/login/onboarding-completion",
                "http://bank.nerva.example/web/auth/login/onboarding-completion",
                "http://user@localhost:8085/web/auth/login/onboarding-completion",
                "http://localhost:8085/web/auth/login/onboarding-completion?returnTo=/app",
                "http://localhost:8085/web/auth/login/onboarding-completion#fragment",
                "http://localhost:8085/web/auth/login/onboarding-completion/",
                "http://localhost:8085/web/auth/login/%6fnboarding-completion",
                "http://localhost.:8085/web/auth/login/onboarding-completion",
                "http://local%68ost:8085/web/auth/login/onboarding-completion",
                "http://localhost:8085\\@attacker.example/web/auth/login/onboarding-completion",
                "http://localhost:0/web/auth/login/onboarding-completion",
                "http://localhost:65536/web/auth/login/onboarding-completion",
                "http://localhost:not-a-port/web/auth/login/onboarding-completion"
        );

        invalidRedirectUris.forEach(redirectUri -> assertThatThrownBy(
                () -> adapter(client, redirectUri)
        ).as("redirect URI %s", redirectUri)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("onboarding.keycloak.credential-redirect-uri"));
    }

    private KeycloakCredentialProvisioningAdapter adapter(KeycloakAdminFeignClient client) {
        return adapter(client, "http://localhost:8085/web/auth/login/onboarding-completion");
    }

    private KeycloakCredentialProvisioningAdapter adapter(
            KeycloakAdminFeignClient client,
            String redirectUri
    ) {
        return adapter(client, redirectUri, OpenTelemetry.noop());
    }

    private KeycloakCredentialProvisioningAdapter adapter(
            KeycloakAdminFeignClient client,
            String redirectUri,
            OpenTelemetry openTelemetry
    ) {
        return new KeycloakCredentialProvisioningAdapter(
                client,
                "banking-ecosystem",
                "home-banking-bff",
                redirectUri,
                Duration.ofHours(24),
                openTelemetry
        );
    }

    private OpenTelemetry openTelemetry(InMemorySpanExporter exporter) {
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }

    private ApplicantData applicant(UUID applicationId) {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        return ApplicantData.create(applicationId, "Federico", null, "Bacelar", LocalDate.of(1990, 1, 1),
                "AR", ApplicantDocumentType.DNI, "30111222", "AR", LocalDate.of(2030, 1, 1),
                "+5491111111111", "Calle", "1", "Ciudad", "Buenos Aires", "1000", "AR", now);
    }
}
