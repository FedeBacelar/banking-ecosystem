package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto.CreateIdentityLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto.ProvisionedIdentityResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "identity-service")
public interface IdentityFeignClient {
    @PostMapping("/identity-links")
    ProvisionedIdentityResponse create(@RequestBody CreateIdentityLinkRequest request);
    @GetMapping("/identity-links/providers/KEYCLOAK/subjects/{subject}")
    ProvisionedIdentityResponse getBySubject(@PathVariable String subject);
}
