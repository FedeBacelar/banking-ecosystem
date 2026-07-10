package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.CustomerLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.RegisterCustomerRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.ProvisionedCustomerResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto.ApproveCustomerKycRequest;
import java.util.UUID;

@FeignClient(name = "customer-service")
public interface CustomerFeignClient {
    @GetMapping("/customers/by-document")
    CustomerLookupResponse findByDocument(@RequestParam String type, @RequestParam String number, @RequestParam String country);

    @GetMapping("/customers/by-email")
    CustomerLookupResponse findByEmail(@RequestParam String email);

    @PostMapping("/customers/natural-persons")
    ProvisionedCustomerResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody RegisterCustomerRequest request);

    @PatchMapping("/customers/{customerId}/kyc/approve")
    ProvisionedCustomerResponse approveKyc(@PathVariable UUID customerId, @RequestBody ApproveCustomerKycRequest request);

    @GetMapping("/customers/{customerId}")
    ProvisionedCustomerResponse get(@PathVariable UUID customerId);
}
