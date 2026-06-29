package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.command.AccountAliasCommand;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import java.util.UUID;

public interface UpdateAccountAliasUseCase {

    AccountDetails updateAlias(UUID accountId, AccountAliasCommand command);
}
