package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakCredentialResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakRoleResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto.KeycloakUserResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "keycloak-admin", url = "${onboarding.keycloak.admin-base-url:http://localhost:8090}")
public interface KeycloakAdminFeignClient {
    @PostMapping("/admin/realms/{realm}/users")
    ResponseEntity<Void> createUser(@PathVariable String realm, @RequestBody KeycloakUserRequest request);

    @GetMapping("/admin/realms/{realm}/users")
    List<KeycloakUserResponse> findUsers(@PathVariable String realm, @RequestParam String email, @RequestParam boolean exact);

    @GetMapping("/admin/realms/{realm}/users/{userId}")
    KeycloakUserResponse getUser(@PathVariable String realm, @PathVariable String userId);

    @GetMapping("/admin/realms/{realm}/users/{userId}/credentials")
    List<KeycloakCredentialResponse> getCredentials(@PathVariable String realm, @PathVariable String userId);

    @GetMapping("/admin/realms/{realm}/roles/{roleName}")
    KeycloakRoleResponse getRole(@PathVariable String realm, @PathVariable String roleName);

    @PostMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
    void assignRealmRoles(@PathVariable String realm, @PathVariable String userId, @RequestBody List<KeycloakRoleResponse> roles);

    @PutMapping("/admin/realms/{realm}/users/{userId}/execute-actions-email")
    void executeActionsEmail(
            @PathVariable String realm,
            @PathVariable String userId,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "redirect_uri") String redirectUri,
            @RequestParam int lifespan,
            @RequestBody List<String> actions
    );
}
