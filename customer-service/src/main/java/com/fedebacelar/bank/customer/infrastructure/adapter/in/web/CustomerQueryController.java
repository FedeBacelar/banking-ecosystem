package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import com.fedebacelar.bank.customer.application.port.in.FindCustomerByDocumentUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByEmailUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByNumberUseCase;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerStatusHistoryUseCase;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerUseCase;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerStatusHistoryResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@Validated
public class CustomerQueryController {

    private final GetCustomerUseCase getCustomerUseCase;
    private final FindCustomerByDocumentUseCase findCustomerByDocumentUseCase;
    private final FindCustomerByEmailUseCase findCustomerByEmailUseCase;
    private final FindCustomerByNumberUseCase findCustomerByNumberUseCase;
    private final GetCustomerStatusHistoryUseCase getCustomerStatusHistoryUseCase;
    private final CustomerWebMapper mapper;

    public CustomerQueryController(
            GetCustomerUseCase getCustomerUseCase,
            FindCustomerByDocumentUseCase findCustomerByDocumentUseCase,
            FindCustomerByEmailUseCase findCustomerByEmailUseCase,
            FindCustomerByNumberUseCase findCustomerByNumberUseCase,
            GetCustomerStatusHistoryUseCase getCustomerStatusHistoryUseCase,
            CustomerWebMapper mapper
    ) {
        this.getCustomerUseCase = getCustomerUseCase;
        this.findCustomerByDocumentUseCase = findCustomerByDocumentUseCase;
        this.findCustomerByEmailUseCase = findCustomerByEmailUseCase;
        this.findCustomerByNumberUseCase = findCustomerByNumberUseCase;
        this.getCustomerStatusHistoryUseCase = getCustomerStatusHistoryUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Get customer by id")
    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable UUID customerId) {
        return mapper.toResponse(getCustomerUseCase.getCustomer(customerId));
    }

    @Operation(summary = "Get customer by document")
    @GetMapping("/by-document")
    public CustomerResponse getByDocument(
            @RequestParam DocumentType type,
            @RequestParam String number,
            @RequestParam @Pattern(regexp = "^[A-Z]{2}$") String country
    ) {
        return mapper.toResponse(findCustomerByDocumentUseCase.findByDocument(type, number, country));
    }

    @Operation(summary = "Get customer by email")
    @GetMapping("/by-email")
    public CustomerResponse getByEmail(@RequestParam String email) {
        return mapper.toResponse(findCustomerByEmailUseCase.findByEmail(email));
    }

    @Operation(summary = "Get customer by customer number")
    @GetMapping("/by-number/{customerNumber}")
    public CustomerResponse getByCustomerNumber(@PathVariable String customerNumber) {
        return mapper.toResponse(findCustomerByNumberUseCase.findByCustomerNumber(customerNumber));
    }

    @Operation(summary = "Get customer status history")
    @GetMapping("/{customerId}/status-history")
    public List<CustomerStatusHistoryResponse> getStatusHistory(@PathVariable UUID customerId) {
        return getCustomerStatusHistoryUseCase.getStatusHistory(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
