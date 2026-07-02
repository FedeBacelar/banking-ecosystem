package com.fedebacelar.bank.identity.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.identity.application.port.in.ChangeIdentityLinkStatusUseCase;
import com.fedebacelar.bank.identity.application.port.in.CreateIdentityLinkUseCase;
import com.fedebacelar.bank.identity.application.port.in.GetCustomerIdentityLinksUseCase;
import com.fedebacelar.bank.identity.application.port.in.ResolveIdentityLinkUseCase;
import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.exception.DuplicateIdentityLinkException;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.mapper.IdentityLinkWebMapper;
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

@WebMvcTest(value = IdentityLinkController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({IdentityLinkWebMapper.class, GlobalExceptionHandler.class})
class IdentityLinkWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateIdentityLinkUseCase createIdentityLinkUseCase;

    @MockitoBean
    private ResolveIdentityLinkUseCase resolveIdentityLinkUseCase;

    @MockitoBean
    private GetCustomerIdentityLinksUseCase getCustomerIdentityLinksUseCase;

    @MockitoBean
    private ChangeIdentityLinkStatusUseCase changeIdentityLinkStatusUseCase;

    @Test
    void createsIdentityLink() throws Exception {
        when(createIdentityLinkUseCase.create(any())).thenReturn(details(IdentityLinkStatus.ACTIVE));

        mockMvc.perform(post("/identity-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "22222222-2222-2222-2222-222222222222",
                                  "provider": "KEYCLOAK",
                                  "providerSubject": "keycloak-sub-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("KEYCLOAK"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returnsBadRequestForInvalidCreateBody() throws Exception {
        mockMvc.perform(post("/identity-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": null,
                                  "provider": "KEYCLOAK",
                                  "providerSubject": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsConflictForDuplicatedIdentityLink() throws Exception {
        when(createIdentityLinkUseCase.create(any()))
                .thenThrow(new DuplicateIdentityLinkException(IdentityProvider.KEYCLOAK, "keycloak-sub-1"));

        mockMvc.perform(post("/identity-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "22222222-2222-2222-2222-222222222222",
                                  "provider": "KEYCLOAK",
                                  "providerSubject": "keycloak-sub-1"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate identity link"));
    }

    @Test
    void resolvesActiveIdentityLink() throws Exception {
        when(resolveIdentityLinkUseCase.resolveActive(IdentityProvider.KEYCLOAK, "keycloak-sub-1"))
                .thenReturn(details(IdentityLinkStatus.ACTIVE));

        mockMvc.perform(get("/identity-links/providers/KEYCLOAK/subjects/keycloak-sub-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void returnsCustomerIdentityLinks() throws Exception {
        UUID customerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(getCustomerIdentityLinksUseCase.getByCustomerId(customerId)).thenReturn(List.of(details(IdentityLinkStatus.ACTIVE)));

        mockMvc.perform(get("/identity-links/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("KEYCLOAK"));
    }

    @Test
    void disablesIdentityLink() throws Exception {
        UUID identityLinkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(changeIdentityLinkStatusUseCase.disable(identityLinkId)).thenReturn(details(IdentityLinkStatus.DISABLED));

        mockMvc.perform(patch("/identity-links/{identityLinkId}/disable", identityLinkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    private IdentityLinkDetails details(IdentityLinkStatus status) {
        return new IdentityLinkDetails(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                IdentityProvider.KEYCLOAK,
                "keycloak-sub-1",
                status,
                Instant.parse("2026-07-02T00:00:00Z"),
                Instant.parse("2026-07-02T00:00:00Z"),
                0L
        );
    }
}
