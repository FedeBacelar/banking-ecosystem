package com.fedebacelar.bank.identity.application.port.in;

import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import java.util.UUID;

public interface ChangeIdentityLinkStatusUseCase {

    IdentityLinkDetails activate(UUID identityLinkId);

    IdentityLinkDetails disable(UUID identityLinkId);
}
