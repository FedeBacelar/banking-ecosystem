package com.fedebacelar.bank.account.infrastructure.adapter.in.web;

import com.fedebacelar.bank.account.application.port.in.AccountLifecycleUseCase;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountReasonRequest;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper.AccountWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountLifecycleController {

    private final AccountLifecycleUseCase accountLifecycleUseCase;
    private final AccountWebMapper mapper;

    public AccountLifecycleController(AccountLifecycleUseCase accountLifecycleUseCase, AccountWebMapper mapper) {
        this.accountLifecycleUseCase = accountLifecycleUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Activate account")
    @PatchMapping("/{accountId}/activate")
    public AccountResponse activate(@PathVariable UUID accountId, @Valid @RequestBody AccountReasonRequest request) {
        return mapper.toResponse(accountLifecycleUseCase.activate(accountId, mapper.toCommand(request)));
    }

    @Operation(summary = "Unfreeze account")
    @PatchMapping("/{accountId}/unfreeze")
    public AccountResponse unfreeze(@PathVariable UUID accountId, @Valid @RequestBody AccountReasonRequest request) {
        return mapper.toResponse(accountLifecycleUseCase.activate(accountId, mapper.toCommand(request)));
    }

    @Operation(summary = "Freeze account")
    @PatchMapping("/{accountId}/freeze")
    public AccountResponse freeze(@PathVariable UUID accountId, @Valid @RequestBody AccountReasonRequest request) {
        return mapper.toResponse(accountLifecycleUseCase.freeze(accountId, mapper.toCommand(request)));
    }

    @Operation(summary = "Close account")
    @PatchMapping("/{accountId}/close")
    public AccountResponse close(@PathVariable UUID accountId, @Valid @RequestBody AccountReasonRequest request) {
        return mapper.toResponse(accountLifecycleUseCase.close(accountId, mapper.toCommand(request)));
    }
}
