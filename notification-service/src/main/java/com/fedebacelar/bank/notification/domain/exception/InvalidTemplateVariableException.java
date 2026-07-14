package com.fedebacelar.bank.notification.domain.exception;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;

public class InvalidTemplateVariableException extends RuntimeException {

    public InvalidTemplateVariableException(NotificationTemplateCode templateCode, String variableName) {
        super("Invalid variable '" + variableName + "' for template " + templateCode);
    }
}
