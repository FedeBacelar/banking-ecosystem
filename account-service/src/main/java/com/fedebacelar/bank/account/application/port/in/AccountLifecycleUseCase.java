package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.command.AccountReasonCommand;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import java.util.UUID;

public interface AccountLifecycleUseCase {

    AccountDetails activate(UUID accountId, AccountReasonCommand command);

    AccountDetails freeze(UUID accountId, AccountReasonCommand command);

    AccountDetails close(UUID accountId, AccountReasonCommand command);
}
