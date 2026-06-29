package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.exception.InvalidCustomerStatusTransitionException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NaturalPersonCustomer(
        Party party,
        NaturalPerson naturalPerson,
        Customer customer,
        IdentificationDocument primaryDocument,
        List<ContactPoint> contactPoints,
        List<Address> addresses,
        KycProfile kycProfile,
        List<CustomerStatusHistory> statusHistory
) {

    public NaturalPersonCustomer withStatus(CustomerStatus newStatus, String reason, Instant changedAt) {
        CustomerStatus previousStatus = customer.status();
        validateTransition(previousStatus, newStatus);
        Customer updatedCustomer = customer.withStatus(newStatus, changedAt);
        return withCustomerAndKyc(updatedCustomer, kycProfile, previousStatus, newStatus, reason, changedAt);
    }

    public NaturalPersonCustomer approveKyc(Instant reviewedAt) {
        validateTransition(customer.status(), CustomerStatus.ACTIVE);
        Customer updatedCustomer = customer.withStatus(CustomerStatus.ACTIVE, reviewedAt);
        KycProfile updatedKycProfile = kycProfile.approve(reviewedAt);
        return withCustomerAndKyc(updatedCustomer, updatedKycProfile, customer.status(), CustomerStatus.ACTIVE, "KYC approved", reviewedAt);
    }

    public NaturalPersonCustomer rejectKyc(String reason, Instant reviewedAt) {
        validateTransition(customer.status(), CustomerStatus.CLOSED);
        Customer updatedCustomer = customer.withStatus(CustomerStatus.CLOSED, reviewedAt);
        KycProfile updatedKycProfile = kycProfile.reject(reviewedAt);
        return withCustomerAndKyc(updatedCustomer, updatedKycProfile, customer.status(), CustomerStatus.CLOSED, reason, reviewedAt);
    }

    public NaturalPersonCustomer suspend(String reason, Instant changedAt) {
        return withStatus(CustomerStatus.SUSPENDED, reason, changedAt);
    }

    public NaturalPersonCustomer reactivate(String reason, Instant changedAt) {
        return withStatus(CustomerStatus.ACTIVE, reason, changedAt);
    }

    public NaturalPersonCustomer close(String reason, Instant changedAt) {
        return withStatus(CustomerStatus.CLOSED, reason, changedAt);
    }

    private NaturalPersonCustomer withCustomerAndKyc(
            Customer updatedCustomer,
            KycProfile updatedKycProfile,
            CustomerStatus previousStatus,
            CustomerStatus newStatus,
            String reason,
            Instant changedAt
    ) {
        CustomerStatusHistory entry = new CustomerStatusHistory(
                UUID.randomUUID(),
                customer.id(),
                previousStatus,
                newStatus,
                reason,
                changedAt
        );
        List<CustomerStatusHistory> updatedHistory = new java.util.ArrayList<>(statusHistory);
        updatedHistory.add(entry);
        return new NaturalPersonCustomer(
                party,
                naturalPerson,
                updatedCustomer,
                primaryDocument,
                contactPoints,
                addresses,
                updatedKycProfile,
                List.copyOf(updatedHistory)
        );
    }

    private void validateTransition(CustomerStatus currentStatus, CustomerStatus requestedStatus) {
        boolean valid = switch (currentStatus) {
            case PENDING_KYC -> requestedStatus == CustomerStatus.ACTIVE || requestedStatus == CustomerStatus.CLOSED;
            case ACTIVE -> requestedStatus == CustomerStatus.SUSPENDED || requestedStatus == CustomerStatus.CLOSED;
            case SUSPENDED -> requestedStatus == CustomerStatus.ACTIVE || requestedStatus == CustomerStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new InvalidCustomerStatusTransitionException(currentStatus, requestedStatus);
        }
    }
}
