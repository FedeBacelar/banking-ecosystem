package com.fedebacelar.bank.document.infrastructure.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = {
        SecurityConfigTest.TestDocumentController.class,
        SecurityConfigTest.TestActuatorController.class
})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        SecurityConfigTest.TestDocumentController.class,
        SecurityConfigTest.TestActuatorController.class
})
@TestPropertySource(properties = "banking.security.public-docs-enabled=false")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void documentEndpointsStillRequireTheirMachineRoles() throws Exception {
        mockMvc.perform(post("/internal/documents"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/documents")
                        .header("Authorization", "Bearer " + tokenWithRoles("DOCUMENT_WRITE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/internal/documents/document-1")
                        .header("Authorization", "Bearer " + tokenWithRoles("DOCUMENT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void prometheusDoesNotRequireACustomerToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void healthDoesNotRequireACustomerToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private String tokenWithRoles(String... roles) {
        String token = String.join("-", roles).toLowerCase() + "-token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer("http://localhost:8090/realms/banking-ecosystem")
                .subject("onboarding-service")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return token;
    }

    @RestController
    @RequestMapping("/internal/documents")
    static class TestDocumentController {

        @PostMapping
        void upload() {
        }

        @GetMapping("/{documentId}")
        void getDocument() {
        }
    }

    @RestController
    @RequestMapping("/actuator")
    static class TestActuatorController {

        @GetMapping("/health")
        void health() {
        }

        @GetMapping("/prometheus")
        void prometheus() {
        }
    }
}
