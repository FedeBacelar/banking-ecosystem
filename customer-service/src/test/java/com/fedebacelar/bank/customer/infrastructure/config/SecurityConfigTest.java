package com.fedebacelar.bank.customer.infrastructure.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.customer.application.port.in.ApproveCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.in.CloseCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByDocumentUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByEmailUseCase;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByNumberUseCase;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerStatusHistoryUseCase;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.ReactivateCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerIdempotentlyUseCase;
import com.fedebacelar.bank.customer.application.port.in.RejectCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.in.SuspendCustomerUseCase;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper.CustomerWebMapper;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.RequestFingerprint;
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

@WebMvcTest(controllers = {
        SecurityConfigTest.TestCustomerController.class,
        SecurityConfigTest.TestCustomerProvisioningController.class,
        SecurityConfigTest.TestActuatorController.class,
        SecurityConfigTest.TestDocsController.class
})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        CustomerWebMapper.class,
        RequestFingerprint.class,
        SecurityConfigTest.TestCustomerController.class,
        SecurityConfigTest.TestCustomerProvisioningController.class,
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
    private RegisterNaturalPersonCustomerIdempotentlyUseCase registerNaturalPersonCustomerUseCase;

    @MockitoBean
    private FindCustomerByEmailUseCase findCustomerByEmailUseCase;

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
    void shouldRejectCustomerRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/customers/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCustomerReadWithCustomerReadRole() throws Exception {
        mockMvc.perform(get("/customers/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectCustomerReadWithoutCustomerReadRole() throws Exception {
        mockMvc.perform(get("/customers/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("ACCOUNT_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCustomerWriteWithCustomerWriteRole() throws Exception {
        mockMvc.perform(post("/customers/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireTokenToProvisionCustomer() throws Exception {
        mockMvc.perform(post("/customers/natural-persons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCustomerCreationWithCustomerProvisionRole() throws Exception {
        mockMvc.perform(post("/customers/natural-persons")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_PROVISION")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowKycApprovalWithCustomerProvisionRole() throws Exception {
        mockMvc.perform(patch("/customers/customer-1/kyc/approve")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_PROVISION")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectCustomerCreationWithAdministrativeWriteOnly() throws Exception {
        mockMvc.perform(post("/customers/natural-persons")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_WRITE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdministrativeMutationWithCustomerProvisionRole() throws Exception {
        mockMvc.perform(patch("/customers/customer-1/kyc/reject")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_PROVISION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdministrativeMutationWithCustomerWriteRole() throws Exception {
        mockMvc.perform(patch("/customers/customer-1/kyc/reject")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnsupportedCustomerMethodEvenWithWriteRole() throws Exception {
        mockMvc.perform(delete("/customers/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_WRITE")))
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
    @RequestMapping("/customers/test")
    public static class TestCustomerController {

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
    @RequestMapping("/customers")
    public static class TestCustomerProvisioningController {

        @PostMapping("/natural-persons")
        void createNaturalPerson() {
        }

        @PatchMapping("/{customerId}/kyc/approve")
        void approveKyc() {
        }

        @PatchMapping("/{customerId}/kyc/reject")
        void rejectKyc() {
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
