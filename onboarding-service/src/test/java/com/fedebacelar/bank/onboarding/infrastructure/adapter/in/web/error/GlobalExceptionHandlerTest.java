package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentTooLargeException;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialInvitationCooldownException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnAStablePayloadTooLargeContract() {
        ProblemDetail problem = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(10L * 1024L * 1024L)
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getDetail()).isEqualTo("Each document must be no larger than 10 MB.");
        assertThat(problem.getProperties()).containsEntry("code", "ONBOARDING_DOCUMENT_TOO_LARGE");
    }

    @Test
    void shouldPreservePayloadTooLargeFromTheDocumentService() {
        ProblemDetail problem = handler.handleDocumentTooLarge(
                new OnboardingDocumentTooLargeException(new IllegalStateException("downstream"))
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getDetail()).isEqualTo("Each document must be no larger than 10 MB.");
        assertThat(problem.getProperties()).containsEntry("code", "ONBOARDING_DOCUMENT_TOO_LARGE");
    }

    @Test
    void shouldExposeRetryAfterForCredentialInvitationCooldown() {
        var response = handler.handleCredentialInvitationCooldown(
                new CredentialInvitationCooldownException(45L)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("45");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties())
                .containsEntry("code", "CREDENTIAL_INVITATION_COOLDOWN")
                .containsEntry("retryAfterSeconds", 45L);
    }
}
