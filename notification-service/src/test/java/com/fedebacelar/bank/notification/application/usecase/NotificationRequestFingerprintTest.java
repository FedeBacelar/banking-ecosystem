package com.fedebacelar.bank.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationRequestFingerprintTest {

    @Test
    void isStableAcrossVariableIterationOrderAndSensitiveToRequestIdentity() {
        Map<String, String> firstOrder = new LinkedHashMap<>();
        firstOrder.put("magicLink", "https://app.example/onboarding/continue#token=" + "A".repeat(43));
        firstOrder.put("expiresInMinutes", "30");

        Map<String, String> secondOrder = new LinkedHashMap<>();
        secondOrder.put("expiresInMinutes", "30");
        secondOrder.put("magicLink", "https://app.example/onboarding/continue#token=" + "A".repeat(43));

        String fingerprint = NotificationRequestFingerprint.calculate(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                firstOrder
        );

        assertThat(fingerprint)
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isEqualTo(NotificationRequestFingerprint.calculate(
                        "person@example.com",
                        NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                        secondOrder
                ))
                .isNotEqualTo(NotificationRequestFingerprint.calculate(
                        "other@example.com",
                        NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                        secondOrder
                ));
    }
}
