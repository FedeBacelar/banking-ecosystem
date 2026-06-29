package com.fedebacelar.bank.account.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.account.application.command.AccountAliasCommand;
import com.fedebacelar.bank.account.application.command.AccountReasonCommand;
import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.view.AccountBalanceDetails;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.application.view.AccountStatusHistoryDetails;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountBalanceResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountReasonRequest;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.AccountStatusHistoryResponse;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.OpenAccountRequest;
import com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto.UpdateAccountAliasRequest;
import org.springframework.stereotype.Component;

@Component
public class AccountWebMapper {

    public OpenAccountCommand toCommand(OpenAccountRequest request) {
        return new OpenAccountCommand(request.customerId(), request.type(), request.currency(), request.alias());
    }

    public AccountAliasCommand toCommand(UpdateAccountAliasRequest request) {
        return new AccountAliasCommand(request.alias());
    }

    public AccountReasonCommand toCommand(AccountReasonRequest request) {
        return new AccountReasonCommand(request.reason(), request.changedBy());
    }

    public AccountResponse toResponse(AccountDetails details) {
        return new AccountResponse(
                details.accountId(),
                details.customerId(),
                details.accountNumber(),
                details.cbu(),
                details.alias(),
                details.type(),
                details.currency(),
                details.status(),
                details.openedAt(),
                details.closedAt(),
                toResponse(details.balance())
        );
    }

    public AccountBalanceResponse toResponse(AccountBalanceDetails details) {
        return new AccountBalanceResponse(
                details.accountId(),
                details.currency(),
                details.currentBalance(),
                details.availableBalance(),
                details.holdBalance(),
                details.updatedAt()
        );
    }

    public AccountStatusHistoryResponse toResponse(AccountStatusHistoryDetails details) {
        return new AccountStatusHistoryResponse(
                details.accountId(),
                details.previousStatus(),
                details.newStatus(),
                details.reason(),
                details.changedBy(),
                details.changedAt()
        );
    }
}
