package com.fedebacelar.bank.customer.application.usecase.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.customer.application.command.CustomerReasonCommand;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.usecase.query.GetCustomerStatusHistoryService;
import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyLifecycleStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyType;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import com.fedebacelar.bank.customer.domain.exception.InvalidCustomerStatusTransitionException;
import com.fedebacelar.bank.customer.domain.model.Customer;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;
import com.fedebacelar.bank.customer.domain.model.IdentificationDocument;
import com.fedebacelar.bank.customer.domain.model.KycProfile;
import com.fedebacelar.bank.customer.domain.model.NaturalPerson;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import com.fedebacelar.bank.customer.domain.model.Party;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerLifecycleUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @Test
    void approvesKycUseCase() {
        UUID customerId = UUID.randomUUID();
        when(customerRepositoryPort.findByCustomerId(customerId)).thenReturn(Optional.of(aggregate(customerId, CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW)));
        when(customerRepositoryPort.save(any(NaturalPersonCustomer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new ApproveCustomerKycService(customerRepositoryPort, CLOCK).approveKyc(customerId);

        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(response.kycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    @Test
    void rejectsKycUseCase() {
        UUID customerId = UUID.randomUUID();
        when(customerRepositoryPort.findByCustomerId(customerId)).thenReturn(Optional.of(aggregate(customerId, CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW)));
        when(customerRepositoryPort.save(any(NaturalPersonCustomer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new RejectCustomerKycService(customerRepositoryPort, CLOCK).rejectKyc(new CustomerReasonCommand(customerId, "KYC rejected"));

        assertThat(response.status()).isEqualTo(CustomerStatus.CLOSED);
        assertThat(response.kycStatus()).isEqualTo(KycStatus.REJECTED);
    }

    @Test
    void rejectsInvalidSuspensionFromPendingKyc() {
        UUID customerId = UUID.randomUUID();
        when(customerRepositoryPort.findByCustomerId(customerId)).thenReturn(Optional.of(aggregate(customerId, CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> new SuspendCustomerService(customerRepositoryPort, CLOCK).suspend(new CustomerReasonCommand(customerId, "risk alert")))
                .isInstanceOf(InvalidCustomerStatusTransitionException.class);
    }

    @Test
    void returnsStatusHistory() {
        UUID customerId = UUID.randomUUID();
        when(customerRepositoryPort.findByCustomerId(customerId)).thenReturn(Optional.of(aggregate(customerId, CustomerStatus.ACTIVE, KycStatus.APPROVED)));
        when(customerRepositoryPort.findStatusHistory(customerId)).thenReturn(List.of(
                new CustomerStatusHistory(UUID.randomUUID(), customerId, null, CustomerStatus.PENDING_KYC, "created", Instant.now()),
                new CustomerStatusHistory(UUID.randomUUID(), customerId, CustomerStatus.PENDING_KYC, CustomerStatus.ACTIVE, "KYC approved", Instant.now())
        ));

        var response = new GetCustomerStatusHistoryService(customerRepositoryPort).getStatusHistory(customerId);

        assertThat(response).hasSize(2);
        assertThat(response.get(1).newStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    private NaturalPersonCustomer aggregate(UUID customerId, CustomerStatus customerStatus, KycStatus kycStatus) {
        Instant now = Instant.now();
        UUID partyId = UUID.randomUUID();
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
