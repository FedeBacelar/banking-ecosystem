package com.fedebacelar.bank.account.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.account.application.port.in.AccountLifecycleUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountBalanceUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountStatusHistoryUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountUseCase;
import com.fedebacelar.bank.account.application.port.in.GetCustomerAccountsUseCase;
import com.fedebacelar.bank.account.application.port.in.OpenAccountUseCase;
import com.fedebacelar.bank.account.application.port.in.UpdateAccountAliasUseCase;
import com.fedebacelar.bank.account.application.view.AccountBalanceDetails;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.application.view.AccountStatusHistoryDetails;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.exception.DuplicateAccountAliasException;
import com.fedebacelar.bank.account.domain.exception.InvalidAccountStatusTransitionException;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper.AccountWebMapper;
import java.math.BigDecimal;
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
        AccountOpeningController.class,
        AccountQueryController.class,
        AccountAliasController.class,
        AccountLifecycleController.class
}, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AccountWebMapper.class, GlobalExceptionHandler.class})
class AccountWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenAccountUseCase openAccountUseCase;

    @MockitoBean
    private GetAccountUseCase getAccountUseCase;

    @MockitoBean
    private GetCustomerAccountsUseCase getCustomerAccountsUseCase;

    @MockitoBean
    private GetAccountBalanceUseCase getAccountBalanceUseCase;

    @MockitoBean
    private GetAccountStatusHistoryUseCase getAccountStatusHistoryUseCase;

    @MockitoBean
    private UpdateAccountAliasUseCase updateAccountAliasUseCase;

    @MockitoBean
    private AccountLifecycleUseCase accountLifecycleUseCase;

    @Test
    void opensAccount() throws Exception {
        when(openAccountUseCase.open(any())).thenReturn(accountDetails(AccountStatus.PENDING_ACTIVATION));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "22222222-2222-2222-2222-222222222222",
                                  "type": "SAVINGS",
                                  "currency": "ARS",
                                  "alias": "fede.bank.ars"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_ACTIVATION"))
                .andExpect(jsonPath("$.accountNumber").value("ACC-2026-000001"));
    }

    @Test
    void returnsBadRequestForInvalidOpenAccountBody() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": null,
                                  "type": "SAVINGS",
                                  "currency": "ARS",
                                  "alias": "Invalid Alias"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsBadRequestForTooLongAlias() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "22222222-2222-2222-2222-222222222222",
                                  "type": "SAVINGS",
                                  "currency": "ARS",
                                  "alias": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.ccccc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsConflictForDuplicatedAlias() throws Exception {
        when(openAccountUseCase.open(any())).thenThrow(new DuplicateAccountAliasException("fede.bank.ars"));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "22222222-2222-2222-2222-222222222222",
                                  "type": "SAVINGS",
                                  "currency": "ARS",
                                  "alias": "fede.bank.ars"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate account alias"));
    }

    @Test
    void findsAccountByNumber() throws Exception {
        when(getAccountUseCase.getByNumber("ACC-2026-000001")).thenReturn(accountDetails(AccountStatus.ACTIVE));

        mockMvc.perform(get("/accounts/by-number/ACC-2026-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC-2026-000001"));
    }

    @Test
    void activatesAccount() throws Exception {
        when(accountLifecycleUseCase.activate(any(), any())).thenReturn(accountDetails(AccountStatus.ACTIVE));

        mockMvc.perform(patch("/accounts/11111111-1111-1111-1111-111111111111/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Initial activation",
                                  "changedBy": "system"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returnsConflictForInvalidTransition() throws Exception {
        when(accountLifecycleUseCase.freeze(any(), any()))
                .thenThrow(new InvalidAccountStatusTransitionException(UUID.fromString("11111111-1111-1111-1111-111111111111"), AccountStatus.PENDING_ACTIVATION, AccountStatus.FROZEN));

        mockMvc.perform(patch("/accounts/11111111-1111-1111-1111-111111111111/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Operational review",
                                  "changedBy": "backoffice"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Account conflict"));
    }

    @Test
    void returnsStatusHistory() throws Exception {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(getAccountStatusHistoryUseCase.getStatusHistory(accountId)).thenReturn(List.of(
                new AccountStatusHistoryDetails(accountId, null, AccountStatus.PENDING_ACTIVATION, "created", "system", Instant.now()),
                new AccountStatusHistoryDetails(accountId, AccountStatus.PENDING_ACTIVATION, AccountStatus.ACTIVE, "activated", "system", Instant.now())
        ));

        mockMvc.perform(get("/accounts/{accountId}/status-history", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newStatus").value("PENDING_ACTIVATION"))
                .andExpect(jsonPath("$[1].newStatus").value("ACTIVE"));
    }

    private AccountDetails accountDetails(AccountStatus status) {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        return new AccountDetails(
                accountId,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "ACC-2026-000001",
                "2850001000020260000015",
                "fede.bank.ars",
                AccountType.SAVINGS,
                CurrencyCode.ARS,
                status,
                Instant.parse("2026-06-17T00:00:00Z"),
                null,
                new AccountBalanceDetails(
                        accountId,
                        CurrencyCode.ARS,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Instant.parse("2026-06-17T00:00:00Z")
                )
        );
    }
}
