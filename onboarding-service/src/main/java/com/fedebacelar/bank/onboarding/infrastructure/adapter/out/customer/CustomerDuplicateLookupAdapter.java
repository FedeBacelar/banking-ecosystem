package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer;

import com.fedebacelar.bank.onboarding.application.port.out.CustomerDuplicateLookupPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class CustomerDuplicateLookupAdapter implements CustomerDuplicateLookupPort {
    private final CustomerFeignClient client;

    public CustomerDuplicateLookupAdapter(CustomerFeignClient client) {
        this.client = client;
    }

    @Override
    public boolean existsByDocument(ApplicantDocumentType type, String number, String country) {
        try {
            return client.findByDocument(type.name(), number, country) != null;
        } catch (FeignException.NotFound ignored) {
            return false;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try {
            return client.findByEmail(email) != null;
        } catch (FeignException.NotFound ignored) {
            return false;
        }
    }
}
