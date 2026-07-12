package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account;

import com.fedebacelar.bank.onboarding.application.port.out.AccountProvisioningPort;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto.AccountReasonRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto.OpenAccountRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto.ProvisionedAccountResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;
import feign.FeignException;

@Component
public class AccountProvisioningAdapter implements AccountProvisioningPort {
    private final AccountFeignClient client;
    public AccountProvisioningAdapter(AccountFeignClient client) { this.client = client; }
    @Override public UUID openDefaultAccount(UUID applicationId, UUID customerId) {
        return client.open("onboarding:" + applicationId + ":OPEN_ACCOUNT", new OpenAccountRequest(customerId, "SAVINGS", "ARS", null)).accountId();
    }
    @Override public void activate(UUID accountId) {
        try {
            client.activate(accountId, new AccountReasonRequest("AUTO_ONBOARDING_APPROVED", "onboarding-service"));
        } catch (FeignException.Conflict conflict) {
            if (!isActive(accountId)) throw conflict;
        }
    }
    @Override public boolean isActive(UUID accountId) {
        ProvisionedAccountResponse response = client.get(accountId);
        return response != null && "ACTIVE".equals(response.status());
    }
}
