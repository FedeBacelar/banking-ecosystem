package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.keycloak.dto;

import java.util.List;

public record KeycloakUserResponse(String id, String username, String email, boolean enabled, List<String> requiredActions) {
}
