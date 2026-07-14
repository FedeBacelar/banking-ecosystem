package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeycloakCredentialProvisioningAdapterTest {

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

    private KeycloakCredentialProvisioningAdapter adapter(KeycloakAdminFeignClient client) {
        return new KeycloakCredentialProvisioningAdapter(client, "banking-ecosystem", "home-banking-bff",
                "http://localhost:8085/web/auth/login/onboarding-completion", Duration.ofHours(24));
    }

    private ApplicantData applicant(UUID applicationId) {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        return ApplicantData.create(applicationId, "Federico", null, "Bacelar", LocalDate.of(1990, 1, 1),
                "AR", ApplicantDocumentType.DNI, "30111222", "AR", LocalDate.of(2030, 1, 1),
                "+5491111111111", "Calle", "1", "Ciudad", "Buenos Aires", "1000", "AR", now);
    }
}
