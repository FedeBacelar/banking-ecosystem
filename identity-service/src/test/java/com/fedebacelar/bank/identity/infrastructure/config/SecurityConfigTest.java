package com.fedebacelar.bank.identity.infrastructure.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = {
        SecurityConfigTest.TestIdentityController.class,
        SecurityConfigTest.TestIdentityProvisioningController.class,
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
        SecurityConfigTest.TestIdentityController.class,
        SecurityConfigTest.TestIdentityProvisioningController.class,
        SecurityConfigTest.TestActuatorController.class,
        SecurityConfigTest.TestDocsController.class
})
@TestPropertySource(properties = "banking.security.public-docs-enabled=true")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldRejectIdentityRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/identity-links/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowIdentityReadWithIdentityReadRole() throws Exception {
        mockMvc.perform(get("/identity-links/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectIdentityReadWithoutIdentityReadRole() throws Exception {
        mockMvc.perform(get("/identity-links/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("CUSTOMER_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowIdentityWriteWithIdentityWriteRole() throws Exception {
        mockMvc.perform(post("/identity-links/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireTokenToProvisionIdentityLink() throws Exception {
        mockMvc.perform(post("/identity-links"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowIdentityLinkCreationWithIdentityProvisionRole() throws Exception {
        mockMvc.perform(post("/identity-links")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_PROVISION")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectIdentityLinkCreationWithAdministrativeWriteOnly() throws Exception {
        mockMvc.perform(post("/identity-links")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_WRITE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdministrativeMutationWithIdentityProvisionRole() throws Exception {
        mockMvc.perform(patch("/identity-links/link-1/disable")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_PROVISION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdministrativeMutationWithIdentityWriteRole() throws Exception {
        mockMvc.perform(patch("/identity-links/link-1/disable")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowIdentityPatchWithIdentityWriteRole() throws Exception {
        mockMvc.perform(patch("/identity-links/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnsupportedIdentityMethodEvenWithWriteRole() throws Exception {
        mockMvc.perform(delete("/identity-links/test")
                        .header("Authorization", "Bearer " + tokenWithRoles("IDENTITY_WRITE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldExposeHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposePrometheusWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
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
    @RequestMapping("/identity-links/test")
    public static class TestIdentityController {

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
    @RequestMapping("/identity-links")
    public static class TestIdentityProvisioningController {

        @PostMapping
        void create() {
        }

        @PatchMapping("/{identityLinkId}/disable")
        void disable() {
        }
    }

    @RestController
    @RequestMapping("/actuator")
    public static class TestActuatorController {

        @GetMapping("/health")
        void health() {
        }

        @GetMapping("/prometheus")
        void prometheus() {
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
