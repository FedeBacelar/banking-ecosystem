package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerUseCase;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.RegisterNaturalPersonCustomerRequest;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
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
@RequestMapping("/customers")
@Validated
public class CustomerRegistrationController {

    private final RegisterNaturalPersonCustomerUseCase registerNaturalPersonCustomerUseCase;
    private final CustomerWebMapper mapper;

    public CustomerRegistrationController(
            RegisterNaturalPersonCustomerUseCase registerNaturalPersonCustomerUseCase,
            CustomerWebMapper mapper
    ) {
        this.registerNaturalPersonCustomerUseCase = registerNaturalPersonCustomerUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Register a natural person customer", description = "Creates a party, natural person, primary document, customer relationship and initial pending KYC profile.")
    @ApiResponse(responseCode = "201", description = "Customer registered")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Document already registered")
    @PostMapping("/natural-persons")
    public ResponseEntity<CustomerResponse> registerNaturalPerson(@Valid @RequestBody RegisterNaturalPersonCustomerRequest request) {
        CustomerResponse response = mapper.toResponse(registerNaturalPersonCustomerUseCase.register(mapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/customers/" + response.customerId())).body(response);
    }
}
