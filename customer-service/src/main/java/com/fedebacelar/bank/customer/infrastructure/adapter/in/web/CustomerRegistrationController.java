package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerIdempotentlyUseCase;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.RegisterNaturalPersonCustomerRequest;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
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
@RequestMapping("/customers")
@Validated
public class CustomerRegistrationController {

    private final RegisterNaturalPersonCustomerIdempotentlyUseCase registrationUseCase;
    private final CustomerWebMapper mapper;
    private final RequestFingerprint requestFingerprint;

    public CustomerRegistrationController(
            RegisterNaturalPersonCustomerIdempotentlyUseCase registrationUseCase,
            CustomerWebMapper mapper,
            RequestFingerprint requestFingerprint
    ) {
        this.registrationUseCase = registrationUseCase;
        this.mapper = mapper;
        this.requestFingerprint = requestFingerprint;
    }

    @Operation(summary = "Register a natural person customer", description = "Creates a party, natural person, primary document, customer relationship and initial pending KYC profile.")
    @ApiResponse(responseCode = "201", description = "Customer registered")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Document already registered")
    @PostMapping("/natural-persons")
    public ResponseEntity<CustomerResponse> registerNaturalPerson(
            @RequestHeader(name = "Idempotency-Key", required = false)
            @Size(max = 160)
            @Pattern(regexp = "^[A-Za-z0-9:._-]+$") String idempotencyKey,
            @Valid @RequestBody RegisterNaturalPersonCustomerRequest request
    ) {
        CustomerResponse response = mapper.toResponse(registrationUseCase.register(
                idempotencyKey, requestFingerprint.hash(request), mapper.toCommand(request)
        ));
        return ResponseEntity.created(URI.create("/customers/" + response.customerId())).body(response);
    }
}
