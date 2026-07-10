package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer;

import com.fedebacelar.bank.onboarding.application.port.out.CustomerProvisioningPort;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.ProvisionedCustomerResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.RegisterCustomerRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.ApproveCustomerKycRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;
import feign.FeignException;

@Component
public class CustomerProvisioningAdapter implements CustomerProvisioningPort {
    private final CustomerFeignClient client;
    public CustomerProvisioningAdapter(CustomerFeignClient client) { this.client = client; }
    @Override public UUID createCustomer(UUID applicationId, String email, ApplicantData applicantData) {
        return client.create(idempotencyKey(applicationId, "CREATE_CUSTOMER"), RegisterCustomerRequest.from(email, applicantData)).customerId();
    }
    @Override public void approveKyc(UUID customerId) {
        try {
            client.approveKyc(customerId, new ApproveCustomerKycRequest(
                    "AUTO_ONBOARDING_APPROVED", "onboarding-service"
            ));
        } catch (FeignException.Conflict conflict) {
            if (!isActive(customerId)) throw conflict;
        }
    }
    @Override public boolean isActive(UUID customerId) {
        ProvisionedCustomerResponse response = client.get(customerId);
        return response != null && "ACTIVE".equals(response.status());
    }
    private String idempotencyKey(UUID applicationId, String step) { return "onboarding:" + applicationId + ":" + step; }
}
