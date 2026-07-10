package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.domain.exception.NotificationDeliveryException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.NotificationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.SendEmailNotificationRequest;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationFeignAdapterTest {

    private final NotificationFeignClient notificationFeignClient = mock(NotificationFeignClient.class);
    private final NotificationFeignAdapter adapter = new NotificationFeignAdapter(notificationFeignClient);

    @Test
    void sendsMagicLinkWhenNotificationIsSent() {
        UUID applicationId = UUID.randomUUID();
        when(notificationFeignClient.sendEmail(any())).thenReturn(new NotificationResponse("SENT"));

        adapter.sendMagicLink(applicationId, "person@example.com", "http://localhost/link", Duration.ofMinutes(30));

        verify(notificationFeignClient).sendEmail(any(SendEmailNotificationRequest.class));
    }

    @Test
    void rejectsFailedNotificationDelivery() {
        UUID applicationId = UUID.randomUUID();
        when(notificationFeignClient.sendEmail(any())).thenReturn(new NotificationResponse("FAILED"));

        assertThatThrownBy(() -> adapter.sendMagicLink(applicationId, "person@example.com", "http://localhost/link", Duration.ofMinutes(30)))
                .isInstanceOf(NotificationDeliveryException.class);
    }
}
