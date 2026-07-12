package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.http;

import com.fedebacelar.bank.onboarding.application.port.out.ProvisioningFailureClassifierPort;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialIdentityConflictException;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class FeignProvisioningFailureClassifierAdapter implements ProvisioningFailureClassifierPort {

    @Override
    public boolean isRetryable(RuntimeException exception) {
        if (exception instanceof CredentialIdentityConflictException) {
            return false;
        }
        if (exception instanceof FeignException feignException) {
            int status = feignException.status();
            return status == 429 || status < 0 || status >= 500;
        }
        return false;
    }
}
