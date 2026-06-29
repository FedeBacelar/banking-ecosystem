package com.fedebacelar.bank.customer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyLifecycleStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyType;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import com.fedebacelar.bank.customer.domain.exception.InvalidCustomerStatusTransitionException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NaturalPersonCustomerTest {

    @Test
    void approvesKycFromPendingKyc() {
        NaturalPersonCustomer updated = aggregate(CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW).approveKyc(Instant.now());

        assertThat(updated.customer().status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(updated.kycProfile().status()).isEqualTo(KycStatus.APPROVED);
        assertThat(updated.statusHistory()).hasSize(2);
    }

    @Test
    void rejectsKycAndClosesCustomer() {
        NaturalPersonCustomer updated = aggregate(CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW).rejectKyc("KYC rejected", Instant.now());

        assertThat(updated.customer().status()).isEqualTo(CustomerStatus.CLOSED);
        assertThat(updated.kycProfile().status()).isEqualTo(KycStatus.REJECTED);
    }

    @Test
    void closedCustomerIsTerminal() {
        NaturalPersonCustomer closed = aggregate(CustomerStatus.CLOSED, KycStatus.REJECTED);

        assertThatThrownBy(() -> closed.reactivate("manual review", Instant.now()))
                .isInstanceOf(InvalidCustomerStatusTransitionException.class);
    }

    @Test
    void cannotSuspendPendingKycCustomer() {
        NaturalPersonCustomer pending = aggregate(CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> pending.suspend("risk alert", Instant.now()))
                .isInstanceOf(InvalidCustomerStatusTransitionException.class);
    }

    private NaturalPersonCustomer aggregate(CustomerStatus customerStatus, KycStatus kycStatus) {
        Instant now = Instant.now();
        UUID partyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        return new NaturalPersonCustomer(
                new Party(partyId, PartyType.NATURAL_PERSON, PartyLifecycleStatus.REGISTERED, now, now),
                new NaturalPerson(partyId, "Federico", null, "Bacelar", LocalDate.of(1990, 1, 15), "AR"),
                new Customer(customerId, partyId, "CUS-2026-000001", customerStatus, LocalDate.now(), null, now, now, 0L),
                new IdentificationDocument(UUID.randomUUID(), partyId, DocumentType.DNI, "30111222", "AR", null, true),
                List.of(),
                List.of(),
                new KycProfile(UUID.randomUUID(), customerId, RiskLevel.LOW, kycStatus, null, null),
                List.of(new CustomerStatusHistory(UUID.randomUUID(), customerId, null, customerStatus, "created", now))
        );
    }
}
