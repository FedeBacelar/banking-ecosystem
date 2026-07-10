package com.fedebacelar.bank.account.infrastructure.adapter.in.web;

import com.fedebacelar.bank.account.application.port.in.OpenAccountIdempotentlyUseCase;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.OpenAccountRequest;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper.AccountWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountOpeningController {

    private final OpenAccountIdempotentlyUseCase openAccountUseCase;
    private final AccountWebMapper mapper;
    private final RequestFingerprint requestFingerprint;

    public AccountOpeningController(OpenAccountIdempotentlyUseCase openAccountUseCase, AccountWebMapper mapper, RequestFingerprint requestFingerprint) {
        this.openAccountUseCase = openAccountUseCase;
        this.mapper = mapper;
        this.requestFingerprint = requestFingerprint;
    }

    @Operation(summary = "Open a bank account", description = "Validates the customer, creates an account, initializes its balance and leaves it pending activation.")
    @ApiResponse(responseCode = "201", description = "Account opened")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Alias already registered or customer not eligible")
    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(
            @RequestHeader(name = "Idempotency-Key", required = false)
            @Size(max = 160)
            @Pattern(regexp = "^[A-Za-z0-9:._-]+$") String idempotencyKey,
            @Valid @RequestBody OpenAccountRequest request
    ) {
        AccountResponse response = mapper.toResponse(openAccountUseCase.open(idempotencyKey, requestFingerprint.hash(request), mapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/accounts/" + response.accountId())).body(response);
    }
}
