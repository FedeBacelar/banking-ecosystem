package com.fedebacelar.bank.notification.application.port.in;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.view.NotificationDetails;

public interface SendEmailNotificationUseCase {

    NotificationDetails send(SendEmailNotificationCommand command);
}

