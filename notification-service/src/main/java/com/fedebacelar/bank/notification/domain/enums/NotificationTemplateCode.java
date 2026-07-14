package com.fedebacelar.bank.notification.domain.enums;

public enum NotificationTemplateCode {
    ONBOARDING_EMAIL_MAGIC_LINK(true),
    ONBOARDING_APPROVED_CREDENTIAL_INVITATION(true),
    ONBOARDING_REJECTED(true),
    ONBOARDING_COMPLETED(true);

    private final boolean requiresRedaction;

    NotificationTemplateCode(boolean requiresRedaction) {
        this.requiresRedaction = requiresRedaction;
    }

    public boolean requiresRedaction() {
        return requiresRedaction;
    }
}

