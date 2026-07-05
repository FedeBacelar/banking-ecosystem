package com.fedebacelar.bank.notification.infrastructure.adapter.in.web;

import com.fedebacelar.bank.notification.application.port.in.SendEmailNotificationUseCase;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.dto.NotificationResponse;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.dto.SendEmailNotificationRequest;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.mapper.NotificationWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class NotificationController {

    private final SendEmailNotificationUseCase sendEmailNotificationUseCase;
    private final NotificationWebMapper mapper;

    public NotificationController(SendEmailNotificationUseCase sendEmailNotificationUseCase, NotificationWebMapper mapper) {
        this.sendEmailNotificationUseCase = sendEmailNotificationUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Send templated email notification")
    @PostMapping("/email")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse sendEmail(@Valid @RequestBody SendEmailNotificationRequest request) {
        return mapper.toResponse(sendEmailNotificationUseCase.send(mapper.toCommand(request)));
    }
}

