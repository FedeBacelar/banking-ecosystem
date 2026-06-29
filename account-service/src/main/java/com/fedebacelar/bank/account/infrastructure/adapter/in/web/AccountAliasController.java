package com.fedebacelar.bank.account.infrastructure.adapter.in.web;

import com.fedebacelar.bank.account.application.port.in.UpdateAccountAliasUseCase;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.UpdateAccountAliasRequest;
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
public class AccountAliasController {

    private final UpdateAccountAliasUseCase updateAccountAliasUseCase;
    private final AccountWebMapper mapper;

    public AccountAliasController(UpdateAccountAliasUseCase updateAccountAliasUseCase, AccountWebMapper mapper) {
        this.updateAccountAliasUseCase = updateAccountAliasUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Update account alias")
    @PatchMapping("/{accountId}/alias")
    public AccountResponse updateAlias(@PathVariable UUID accountId, @Valid @RequestBody UpdateAccountAliasRequest request) {
        return mapper.toResponse(updateAccountAliasUseCase.updateAlias(accountId, mapper.toCommand(request)));
    }
}
