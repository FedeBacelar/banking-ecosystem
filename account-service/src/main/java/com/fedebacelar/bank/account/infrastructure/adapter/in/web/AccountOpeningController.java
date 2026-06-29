package com.fedebacelar.bank.account.infrastructure.adapter.in.web;

import com.fedebacelar.bank.account.application.port.in.OpenAccountUseCase;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.OpenAccountRequest;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper.AccountWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountOpeningController {

    private final OpenAccountUseCase openAccountUseCase;
    private final AccountWebMapper mapper;

    public AccountOpeningController(OpenAccountUseCase openAccountUseCase, AccountWebMapper mapper) {
        this.openAccountUseCase = openAccountUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Open a bank account", description = "Validates the customer, creates an account, initializes its balance and leaves it pending activation.")
    @ApiResponse(responseCode = "201", description = "Account opened")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Alias already registered or customer not eligible")
    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        AccountResponse response = mapper.toResponse(openAccountUseCase.open(mapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/accounts/" + response.accountId())).body(response);
    }
}
