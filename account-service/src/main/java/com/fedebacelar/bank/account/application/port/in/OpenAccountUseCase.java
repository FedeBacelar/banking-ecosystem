package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.view.AccountDetails;

public interface OpenAccountUseCase {

    AccountDetails open(OpenAccountCommand command);
}
