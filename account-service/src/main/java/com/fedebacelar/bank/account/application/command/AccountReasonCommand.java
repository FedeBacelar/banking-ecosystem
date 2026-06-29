package com.fedebacelar.bank.account.application.command;

public record AccountReasonCommand(
        String reason,
        String changedBy
) {
}
