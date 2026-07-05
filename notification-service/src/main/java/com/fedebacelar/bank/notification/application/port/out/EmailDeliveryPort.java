package com.fedebacelar.bank.notification.application.port.out;

import com.fedebacelar.bank.notification.domain.model.Notification;

public interface EmailDeliveryPort {

    void deliver(Notification notification);
}

