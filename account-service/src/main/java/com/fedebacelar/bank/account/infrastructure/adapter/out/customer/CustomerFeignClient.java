package com.fedebacelar.bank.account.infrastructure.adapter.out.customer;

import com.fedebacelar.bank.account.infrastructure.adapter.out.customer.dto.CustomerResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerFeignClient {

    @GetMapping("/customers/{customerId}")
    CustomerResponse getCustomerById(@PathVariable UUID customerId);
}
