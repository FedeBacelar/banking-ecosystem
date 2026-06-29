package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import com.fedebacelar.bank.customer.application.port.in.ApproveCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.in.CloseCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.ReactivateCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.RejectCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.in.SuspendCustomerUseCase;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerReasonRequest;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
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
@RequestMapping("/customers")
@Validated
public class CustomerLifecycleController {

    private final ApproveCustomerKycUseCase approveCustomerKycUseCase;
    private final RejectCustomerKycUseCase rejectCustomerKycUseCase;
    private final SuspendCustomerUseCase suspendCustomerUseCase;
    private final ReactivateCustomerUseCase reactivateCustomerUseCase;
    private final CloseCustomerUseCase closeCustomerUseCase;
    private final CustomerWebMapper mapper;

    public CustomerLifecycleController(
            ApproveCustomerKycUseCase approveCustomerKycUseCase,
            RejectCustomerKycUseCase rejectCustomerKycUseCase,
            SuspendCustomerUseCase suspendCustomerUseCase,
            ReactivateCustomerUseCase reactivateCustomerUseCase,
            CloseCustomerUseCase closeCustomerUseCase,
            CustomerWebMapper mapper
    ) {
        this.approveCustomerKycUseCase = approveCustomerKycUseCase;
        this.rejectCustomerKycUseCase = rejectCustomerKycUseCase;
        this.suspendCustomerUseCase = suspendCustomerUseCase;
        this.reactivateCustomerUseCase = reactivateCustomerUseCase;
        this.closeCustomerUseCase = closeCustomerUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Approve customer KYC", description = "Moves a pending KYC customer to ACTIVE and marks KYC as APPROVED.")
    @PatchMapping("/{customerId}/kyc/approve")
    public CustomerResponse approveKyc(@PathVariable UUID customerId) {
        return mapper.toResponse(approveCustomerKycUseCase.approveKyc(customerId));
    }

    @Operation(summary = "Reject customer KYC", description = "Marks KYC as REJECTED and closes the customer.")
    @PatchMapping("/{customerId}/kyc/reject")
    public CustomerResponse rejectKyc(@PathVariable UUID customerId, @Valid @RequestBody CustomerReasonRequest request) {
        return mapper.toResponse(rejectCustomerKycUseCase.rejectKyc(mapper.toCommand(customerId, request)));
    }

    @Operation(summary = "Suspend customer")
    @PatchMapping("/{customerId}/suspend")
    public CustomerResponse suspend(@PathVariable UUID customerId, @Valid @RequestBody CustomerReasonRequest request) {
        return mapper.toResponse(suspendCustomerUseCase.suspend(mapper.toCommand(customerId, request)));
    }

    @Operation(summary = "Reactivate customer")
    @PatchMapping("/{customerId}/reactivate")
    public CustomerResponse reactivate(@PathVariable UUID customerId, @Valid @RequestBody CustomerReasonRequest request) {
        return mapper.toResponse(reactivateCustomerUseCase.reactivate(mapper.toCommand(customerId, request)));
    }

    @Operation(summary = "Close customer")
    @PatchMapping("/{customerId}/close")
    public CustomerResponse close(@PathVariable UUID customerId, @Valid @RequestBody CustomerReasonRequest request) {
        return mapper.toResponse(closeCustomerUseCase.close(mapper.toCommand(customerId, request)));
    }
}
