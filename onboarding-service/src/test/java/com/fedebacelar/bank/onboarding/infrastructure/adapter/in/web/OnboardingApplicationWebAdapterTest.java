package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper.OnboardingWebMapper;
import java.time.Instant;
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

@WebMvcTest(value = OnboardingApplicationController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({OnboardingWebMapper.class, GlobalExceptionHandler.class})
class OnboardingApplicationWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;

    @MockitoBean
    private ConsumeMagicLinkUseCase consumeMagicLinkUseCase;

    @MockitoBean
    private ValidateContinuationUseCase validateContinuationUseCase;

    @MockitoBean
    private GetOnboardingApplicationUseCase getOnboardingApplicationUseCase;

    @Test
    void startsApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(startOnboardingApplicationUseCase.start(any())).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "person@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("EMAIL_VERIFICATION_PENDING"));
    }

    @Test
    void returnsBadRequestForInvalidEmail() throws Exception {
        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsConflictForDuplicateApplication() throws Exception {
        when(startOnboardingApplicationUseCase.start(any()))
                .thenThrow(new DuplicateActiveOnboardingApplicationException("person@example.com"));

        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "person@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate onboarding application"));
    }

    @Test
    void consumesMagicLink() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(consumeMagicLinkUseCase.consume(any())).thenReturn(new OnboardingContinuationDetails(
                applicationId,
                "person@example.com",
                OnboardingApplicationStatus.IN_PROGRESS,
                "continuation-token",
                Instant.parse("2026-07-05T12:00:00Z")
        ));

        mockMvc.perform(post("/internal/onboarding/magic-links/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "magic-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.continuationToken").value("continuation-token"));
    }

    @Test
    void validatesContinuation() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(validateContinuationUseCase.validate(any())).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/continuations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "continuation-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()));
    }

    @Test
    void getsApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(getOnboardingApplicationUseCase.get(applicationId)).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(get("/internal/onboarding/applications/{applicationId}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()));
    }

    private OnboardingApplicationDetails applicationDetails(UUID applicationId) {
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        return new OnboardingApplicationDetails(
                applicationId,
                "person@example.com",
                OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
                now.plusSeconds(1800),
                null,
                null,
                null,
                now.plusSeconds(1296000),
                now,
                now
        );
    }
}
