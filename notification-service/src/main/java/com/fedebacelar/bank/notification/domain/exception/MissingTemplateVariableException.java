package com.fedebacelar.bank.notification.domain.exception;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;

public class MissingTemplateVariableException extends RuntimeException {

    public MissingTemplateVariableException(NotificationTemplateCode templateCode, String variableName) {
        super("Missing variable '" + variableName + "' for template " + templateCode);
    }
}

