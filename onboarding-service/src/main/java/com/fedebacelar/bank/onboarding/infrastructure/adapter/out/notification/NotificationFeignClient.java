package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.SendEmailNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationFeignClient {

    @PostMapping("/internal/notifications/email")
    void sendEmail(@RequestBody SendEmailNotificationRequest request);
}
