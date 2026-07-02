package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.customer.application.port.in.ApproveCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.in.CloseCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByDocumentUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByNumberUseCase;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerStatusHistoryUseCase;
import com.fedebacelar.bank.customer.application.port.in.ReactivateCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.RejectCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.SuspendCustomerUseCase;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.application.view.CustomerStatusHistoryDetails;
import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import com.fedebacelar.bank.customer.domain.exception.DuplicateDocumentException;
import com.fedebacelar.bank.customer.domain.exception.InvalidCustomerStatusTransitionException;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = {
        CustomerRegistrationController.class,
        CustomerQueryController.class,
        CustomerLifecycleController.class
}, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CustomerWebMapper.class, GlobalExceptionHandler.class})
class CustomerWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterNaturalPersonCustomerUseCase registerNaturalPersonCustomerUseCase;

    @MockitoBean
    private GetCustomerUseCase getCustomerUseCase;

    @MockitoBean
    private FindCustomerByDocumentUseCase findCustomerByDocumentUseCase;

    @MockitoBean
    private FindCustomerByNumberUseCase findCustomerByNumberUseCase;

    @MockitoBean
    private GetCustomerStatusHistoryUseCase getCustomerStatusHistoryUseCase;

    @MockitoBean
    private ApproveCustomerKycUseCase approveCustomerKycUseCase;

    @MockitoBean
    private RejectCustomerKycUseCase rejectCustomerKycUseCase;

    @MockitoBean
    private SuspendCustomerUseCase suspendCustomerUseCase;

    @MockitoBean
    private ReactivateCustomerUseCase reactivateCustomerUseCase;

    @MockitoBean
    private CloseCustomerUseCase closeCustomerUseCase;

    @Test
    void registersNaturalPerson() throws Exception {
        when(registerNaturalPersonCustomerUseCase.register(any())).thenReturn(customerDetails());

        mockMvc.perform(post("/customers/natural-persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Federico",
                                  "lastName": "Bacelar",
                                  "birthDate": "1990-01-15",
                                  "nationality": "AR",
                                  "documentType": "DNI",
                                  "documentNumber": "30111222",
                                  "issuingCountry": "AR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_KYC"))
                .andExpect(jsonPath("$.customerNumber").value("CUS-2026-000001"));
    }

    @Test
    void returnsBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/customers/natural-persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "Bacelar",
                                  "birthDate": "2999-01-15",
                                  "nationality": "ARG",
                                  "documentType": "DNI",
                                  "documentNumber": "",
                                  "issuingCountry": "AR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsBadRequestForNullContactValueWithoutServerError() throws Exception {
        mockMvc.perform(post("/customers/natural-persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Federico",
                                  "lastName": "Bacelar",
                                  "birthDate": "1990-01-15",
                                  "nationality": "AR",
                                  "documentType": "DNI",
                                  "documentNumber": "30111222",
                                  "issuingCountry": "AR",
                                  "contactPoints": [
                                    {
                                      "type": "EMAIL",
                                      "value": null
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsBadRequestForTooLongCustomerFields() throws Exception {
        mockMvc.perform(post("/customers/natural-persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                  "lastName": "Bacelar",
                                  "birthDate": "1990-01-15",
                                  "nationality": "AR",
                                  "documentType": "DNI",
                                  "documentNumber": "123456789012345678901234567890123456789012345678901234567890123456789012345678901",
                                  "issuingCountry": "AR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsBadRequestForTooLongNestedCustomerFields() throws Exception {
        mockMvc.perform(post("/customers/natural-persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Federico",
                                  "lastName": "Bacelar",
                                  "birthDate": "1990-01-15",
                                  "nationality": "AR",
                                  "documentType": "DNI",
                                  "documentNumber": "30111222",
                                  "issuingCountry": "AR",
                                  "contactPoints": [
                                    {
                                      "type": "EMAIL",
                                      "value": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@example.com"
                                    }
                                  ],
                                  "addresses": [
                                    {
                                      "type": "HOME",
                                      "street": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                      "streetNumber": "123",
                                      "city": "Buenos Aires",
                                      "province": "Buenos Aires",
                                      "postalCode": "1234567890123456789012345678901",
                                      "country": "AR"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsConflictForDuplicatedDocument() throws Exception {
        when(registerNaturalPersonCustomerUseCase.register(any()))
                .thenThrow(new DuplicateDocumentException(DocumentType.DNI, "30111222", "AR"));

        mockMvc.perform(post("/customers/natural-persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Federico",
                                  "lastName": "Bacelar",
                                  "birthDate": "1990-01-15",
                                  "nationality": "AR",
                                  "documentType": "DNI",
                                  "documentNumber": "30111222",
                                  "issuingCountry": "AR"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate document"));
    }

    @Test
    void findsCustomerByNumber() throws Exception {
        when(findCustomerByNumberUseCase.findByCustomerNumber("CUS-2026-000001")).thenReturn(customerDetails());

        mockMvc.perform(get("/customers/by-number/CUS-2026-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerNumber").value("CUS-2026-000001"));
    }

    @Test
    void approvesKyc() throws Exception {
        when(approveCustomerKycUseCase.approveKyc(UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .thenReturn(customerDetails(CustomerStatus.ACTIVE, KycStatus.APPROVED));

        mockMvc.perform(patch("/customers/11111111-1111-1111-1111-111111111111/kyc/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.kycStatus").value("APPROVED"));
    }

    @Test
    void returnsConflictForInvalidTransition() throws Exception {
        when(suspendCustomerUseCase.suspend(any()))
                .thenThrow(new InvalidCustomerStatusTransitionException(CustomerStatus.PENDING_KYC, CustomerStatus.SUSPENDED));

        mockMvc.perform(patch("/customers/11111111-1111-1111-1111-111111111111/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"risk alert\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid customer status transition"));
    }

    @Test
    void returnsBadRequestForTooLongLifecycleReason() throws Exception {
        mockMvc.perform(patch("/customers/11111111-1111-1111-1111-111111111111/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsStatusHistory() throws Exception {
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(getCustomerStatusHistoryUseCase.getStatusHistory(customerId)).thenReturn(List.of(
                new CustomerStatusHistoryDetails(UUID.randomUUID(), customerId, null, CustomerStatus.PENDING_KYC, "created", Instant.now()),
                new CustomerStatusHistoryDetails(UUID.randomUUID(), customerId, CustomerStatus.PENDING_KYC, CustomerStatus.ACTIVE, "KYC approved", Instant.now())
        ));

        mockMvc.perform(get("/customers/{customerId}/status-history", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newStatus").value("PENDING_KYC"))
                .andExpect(jsonPath("$[1].newStatus").value("ACTIVE"));
    }

    private CustomerDetails customerDetails() {
        return customerDetails(CustomerStatus.PENDING_KYC, KycStatus.PENDING_REVIEW);
    }

    private CustomerDetails customerDetails(CustomerStatus status, KycStatus kycStatus) {
        return new CustomerDetails(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "CUS-2026-000001",
                status,
                "Federico",
                null,
                "Bacelar",
                LocalDate.of(1990, 1, 15),
                "AR",
                DocumentType.DNI,
                "30111222",
                "AR",
                kycStatus,
                RiskLevel.LOW,
                List.of(),
                List.of()
        );
    }
}
