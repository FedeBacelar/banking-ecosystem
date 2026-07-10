package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto.AccountReasonRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto.OpenAccountRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto.ProvisionedAccountResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "account-service")
public interface AccountFeignClient {
    @PostMapping("/accounts")
    ProvisionedAccountResponse open(@RequestHeader("Idempotency-Key") String key, @RequestBody OpenAccountRequest request);
    @PatchMapping("/accounts/{accountId}/activate")
    ProvisionedAccountResponse activate(@PathVariable UUID accountId, @RequestBody AccountReasonRequest request);
    @GetMapping("/accounts/{accountId}")
    ProvisionedAccountResponse get(@PathVariable UUID accountId);
}
