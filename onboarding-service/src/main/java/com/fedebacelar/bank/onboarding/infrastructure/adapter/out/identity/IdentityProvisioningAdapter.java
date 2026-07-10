package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity;

import com.fedebacelar.bank.onboarding.application.port.out.IdentityProvisioningPort;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto.CreateIdentityLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto.ProvisionedIdentityResponse;
import feign.FeignException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdentityProvisioningAdapter implements IdentityProvisioningPort {
    private final IdentityFeignClient client;
    public IdentityProvisioningAdapter(IdentityFeignClient client) { this.client = client; }
    @Override public UUID createOrResolve(UUID customerId, String subject) {
        try {
            return client.create(new CreateIdentityLinkRequest(customerId, "KEYCLOAK", subject)).id();
        } catch (FeignException.Conflict conflict) {
            ProvisionedIdentityResponse existing = client.getBySubject(subject);
            if (!customerId.equals(existing.customerId())) {
                throw conflict;
            }
            return existing.id();
        }
    }
    @Override public boolean isActive(String subject, UUID customerId) {
        ProvisionedIdentityResponse response = client.getBySubject(subject);
        return response != null && customerId.equals(response.customerId()) && "ACTIVE".equals(response.status());
    }
}
