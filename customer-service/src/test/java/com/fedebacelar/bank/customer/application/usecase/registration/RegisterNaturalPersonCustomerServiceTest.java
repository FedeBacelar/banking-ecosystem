package com.fedebacelar.bank.customer.application.usecase.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.customer.application.command.RegisterNaturalPersonCustomerCommand;
import com.fedebacelar.bank.customer.application.port.out.CustomerNumberGeneratorPort;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.port.out.IdentificationDocumentLookupPort;
import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.exception.DuplicateDocumentException;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterNaturalPersonCustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @Mock
    private IdentificationDocumentLookupPort identificationDocumentLookupPort;

    @Mock
    private CustomerNumberGeneratorPort customerNumberGeneratorPort;

    private RegisterNaturalPersonCustomerService service;

    @BeforeEach
    void setUp() {
        service = new RegisterNaturalPersonCustomerService(
                customerRepositoryPort,
                identificationDocumentLookupPort,
                customerNumberGeneratorPort,
                Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void registersNaturalPersonCustomerPendingKyc() {
        RegisterNaturalPersonCustomerCommand command = validCommand();
        when(identificationDocumentLookupPort.existsDocument(DocumentType.DNI, "30111222", "AR")).thenReturn(false);
        when(customerNumberGeneratorPort.nextCustomerNumber()).thenReturn("CUS-2026-000001");
        when(customerRepositoryPort.save(any(NaturalPersonCustomer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(command);

        assertThat(response.status()).isEqualTo(CustomerStatus.PENDING_KYC);
        assertThat(response.kycStatus()).isEqualTo(KycStatus.PENDING_REVIEW);
        assertThat(response.customerNumber()).isEqualTo("CUS-2026-000001");
        assertThat(response.documentNumber()).isEqualTo("30111222");

        ArgumentCaptor<NaturalPersonCustomer> captor = ArgumentCaptor.forClass(NaturalPersonCustomer.class);
        verify(customerRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().statusHistory()).hasSize(1);
        assertThat(captor.getValue().primaryDocument().primaryDocument()).isTrue();
    }

    @Test
    void rejectsDuplicatedDocument() {
        RegisterNaturalPersonCustomerCommand command = validCommand();
        when(identificationDocumentLookupPort.existsDocument(DocumentType.DNI, "30111222", "AR")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(customerRepositoryPort, never()).save(any());
    }

    private RegisterNaturalPersonCustomerCommand validCommand() {
        return new RegisterNaturalPersonCustomerCommand(
                "Federico",
                null,
                "Bacelar",
                LocalDate.of(1990, 1, 15),
                "AR",
                DocumentType.DNI,
                "30111222",
                "AR",
                null,
                List.of(),
                List.of()
        );
    }
}
