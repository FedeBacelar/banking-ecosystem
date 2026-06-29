package com.fedebacelar.bank.account.infrastructure.adapter.in.web;

import com.fedebacelar.bank.account.application.port.in.GetAccountBalanceUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountStatusHistoryUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountUseCase;
import com.fedebacelar.bank.account.application.port.in.GetCustomerAccountsUseCase;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountBalanceResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountStatusHistoryResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper.AccountWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountQueryController {

    private final GetAccountUseCase getAccountUseCase;
    private final GetCustomerAccountsUseCase getCustomerAccountsUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;
    private final GetAccountStatusHistoryUseCase getAccountStatusHistoryUseCase;
    private final AccountWebMapper mapper;

    public AccountQueryController(
            GetAccountUseCase getAccountUseCase,
            GetCustomerAccountsUseCase getCustomerAccountsUseCase,
            GetAccountBalanceUseCase getAccountBalanceUseCase,
            GetAccountStatusHistoryUseCase getAccountStatusHistoryUseCase,
            AccountWebMapper mapper
    ) {
        this.getAccountUseCase = getAccountUseCase;
        this.getCustomerAccountsUseCase = getCustomerAccountsUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
        this.getAccountStatusHistoryUseCase = getAccountStatusHistoryUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Get account by id")
    @GetMapping("/{accountId}")
    public AccountResponse getById(@PathVariable UUID accountId) {
        return mapper.toResponse(getAccountUseCase.getById(accountId));
    }

    @Operation(summary = "Get account by account number")
    @GetMapping("/by-number/{accountNumber}")
    public AccountResponse getByNumber(@PathVariable String accountNumber) {
        return mapper.toResponse(getAccountUseCase.getByNumber(accountNumber));
    }

    @Operation(summary = "Get account by alias")
    @GetMapping("/by-alias/{alias}")
    public AccountResponse getByAlias(@PathVariable String alias) {
        return mapper.toResponse(getAccountUseCase.getByAlias(alias));
    }

    @Operation(summary = "Get customer accounts")
    @GetMapping("/customer/{customerId}")
    public List<AccountResponse> getByCustomerId(@PathVariable UUID customerId) {
        return getCustomerAccountsUseCase.getByCustomerId(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Operation(summary = "Get account balance")
    @GetMapping("/{accountId}/balance")
    public AccountBalanceResponse getBalance(@PathVariable UUID accountId) {
        return mapper.toResponse(getAccountBalanceUseCase.getBalance(accountId));
    }

    @Operation(summary = "Get account status history")
    @GetMapping("/{accountId}/status-history")
    public List<AccountStatusHistoryResponse> getStatusHistory(@PathVariable UUID accountId) {
        return getAccountStatusHistoryUseCase.getStatusHistory(accountId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
