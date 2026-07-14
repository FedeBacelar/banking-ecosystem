package com.fedebacelar.bank.onboarding.infrastructure.config;

import static org.mockito.Mockito.when;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestOnboardingController.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({SecurityConfig.class, SecurityConfigTest.TestOnboardingController.class})
@TestPropertySource(properties = "banking.security.public-docs-enabled=false")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void completionStatusRequiresAMachineToken() throws Exception {
        mockMvc.perform(post("/internal/onboarding/completion-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completionStatusAcceptsOnboardingRead() throws Exception {
        mockMvc.perform(post("/internal/onboarding/completion-status")
                        .header("Authorization", "Bearer " + tokenWithRoles("ONBOARDING_READ"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void completionStatusDoesNotAcceptWriteWithoutRead() throws Exception {
        mockMvc.perform(post("/internal/onboarding/completion-status")
                        .header("Authorization", "Bearer " + tokenWithRoles("ONBOARDING_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherOnboardingMutationsStillRequireWrite() throws Exception {
        mockMvc.perform(post("/internal/onboarding/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("ONBOARDING_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private String tokenWithRoles(String... roles) {
        String token = String.join("-", roles).toLowerCase() + "-token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer("http://localhost:8090/realms/banking-ecosystem")
                .subject("onboarding-bff-service")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return token;
    }

    @RestController
    @RequestMapping("/internal/onboarding")
    public static class TestOnboardingController {

        @PostMapping("/completion-status")
        void completionStatus() {
        }

        @PostMapping("/test")
        void mutate() {
        }
    }
}
