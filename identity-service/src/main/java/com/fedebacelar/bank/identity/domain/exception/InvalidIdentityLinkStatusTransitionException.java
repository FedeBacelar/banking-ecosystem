package com.fedebacelar.bank.identity.domain.exception;

import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import java.util.UUID;

public class InvalidIdentityLinkStatusTransitionException extends RuntimeException {

    public InvalidIdentityLinkStatusTransitionException(UUID identityLinkId, IdentityLinkStatus currentStatus, IdentityLinkStatus targetStatus) {
        super("Cannot change identity link " + identityLinkId + " from " + currentStatus + " to " + targetStatus);
    }
}
