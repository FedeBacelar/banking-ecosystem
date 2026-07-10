package com.fedebacelar.bank.onboarding.application.command;

public record AcceptTermsCommand(
        String continuationToken,
        boolean accepted,
        String termsVersion
) {
}
