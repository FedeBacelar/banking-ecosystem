package com.fedebacelar.bank.account.infrastructure.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.account.application.port.in.AccountLifecycleUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountBalanceUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountStatusHistoryUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountUseCase;
import com.fedebacelar.bank.account.application.port.in.GetCustomerAccountsUseCase;
import com.fedebacelar.bank.account.application.port.in.OpenAccountIdempotentlyUseCase;
import com.fedebacelar.bank.account.application.port.in.UpdateAccountAliasUseCase;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper.AccountWebMapper;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.RequestFingerprint;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        AccountWebMapper.class,
        RequestFingerprint.class,
        SecurityConfigTest.TestAccountController.class,
        SecurityConfigTest.TestActuatorController.class,
        SecurityConfigTest.TestDocsController.class
})
@TestPropertySource(properties = "banking.security.public-docs-enabled=true")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OpenAccountIdempotentlyUseCase openAccountUseCase;

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
    void shouldRejectAccountRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/accounts/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccountReadWithAccountReadRole() throws Exception {
        mockMvc.perform(get("/accounts/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("ACCOUNT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAccountReadWithoutAccountReadRole() throws Exception {
        mockMvc.perform(get("/accounts/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAccountWriteWithAccountWriteRole() throws Exception {
        mockMvc.perform(post("/accounts/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenWithRoles("ACCOUNT_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnsupportedAccountMethodEvenWithWriteRole() throws Exception {
        mockMvc.perform(delete("/accounts/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("ACCOUNT_WRITE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldExposeHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeSwaggerUiWithoutTokenWhenPublicDocsAreEnabled() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeOpenApiDocsWithoutTokenWhenPublicDocsAreEnabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    private String tokenWithRoles(String... roles) {
        String token = String.join("-", roles).toLowerCase() + "-token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer("http://localhost:8090/realms/banking-ecosystem")
                .subject("api-tester")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return token;
    }

    @RestController
    @RequestMapping("/accounts/test")
    public static class TestAccountController {

        @GetMapping
        void read() {
        }

        @PostMapping
        void write() {
        }

        @PatchMapping
        void update() {
        }
    }

    @RestController
    @RequestMapping("/actuator/health")
    public static class TestActuatorController {

        @GetMapping
        void health() {
        }
    }

    @RestController
    public static class TestDocsController {

        @GetMapping("/swagger-ui.html")
        void swaggerUi() {
        }

        @GetMapping("/v3/api-docs")
        void openApiDocs() {
        }
    }
}
