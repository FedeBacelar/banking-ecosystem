package com.fedebacelar.bank.identity.application.port.in;

import com.fedebacelar.bank.identity.application.command.CreateIdentityLinkCommand;
import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;

public interface CreateIdentityLinkUseCase {

    IdentityLinkDetails create(CreateIdentityLinkCommand command);
}
