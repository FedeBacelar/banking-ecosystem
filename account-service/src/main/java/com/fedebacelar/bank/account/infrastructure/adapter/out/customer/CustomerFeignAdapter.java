package com.fedebacelar.bank.account.infrastructure.adapter.out.customer;

import com.fedebacelar.bank.account.application.port.out.CustomerLookupPort;
import com.fedebacelar.bank.account.domain.exception.CustomerLookupException;
import com.fedebacelar.bank.account.domain.model.CustomerRef;
import feign.FeignException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerFeignAdapter implements CustomerLookupPort {

    private final CustomerFeignClient customerFeignClient;

    public CustomerFeignAdapter(CustomerFeignClient customerFeignClient) {
        this.customerFeignClient = customerFeignClient;
    }

    @Override
    public Optional<CustomerRef> findCustomer(UUID customerId) {
        try {
            var response = customerFeignClient.getCustomerById(customerId);
            return Optional.of(new CustomerRef(response.customerId(), response.customerNumber(), response.status()));
        } catch (FeignException.NotFound exception) {
            return Optional.empty();
        } catch (FeignException exception) {
            throw new CustomerLookupException(customerId);
        }
    }
}
