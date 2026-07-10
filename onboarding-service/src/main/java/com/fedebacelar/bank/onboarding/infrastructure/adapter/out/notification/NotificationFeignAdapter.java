package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification;

import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.domain.exception.NotificationDeliveryException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.NotificationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.SendEmailNotificationRequest;
import feign.FeignException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationFeignAdapter implements NotificationPort {

    private static final String MAGIC_LINK_TEMPLATE = "ONBOARDING_EMAIL_MAGIC_LINK";

    private final NotificationFeignClient notificationFeignClient;

    public NotificationFeignAdapter(NotificationFeignClient notificationFeignClient) {
        this.notificationFeignClient = notificationFeignClient;
    }

    @Override
    public void sendMagicLink(UUID applicationId, String recipient, String magicLink, Duration expiresIn) {
        try {
            NotificationResponse response = notificationFeignClient.sendEmail(new SendEmailNotificationRequest(
                    recipient,
                    MAGIC_LINK_TEMPLATE,
                    Map.of(
                            "magicLink", magicLink,
                            "expiresInMinutes", String.valueOf(expiresIn.toMinutes())
                    ),
                    applicationId.toString()
            ));
            if (response == null || !"SENT".equals(response.status())) {
                throw new NotificationDeliveryException(applicationId, "notification service returned status " + (response == null ? "null" : response.status()));
            }
        } catch (FeignException exception) {
            throw new NotificationDeliveryException(applicationId, exception);
        }
    }
}
