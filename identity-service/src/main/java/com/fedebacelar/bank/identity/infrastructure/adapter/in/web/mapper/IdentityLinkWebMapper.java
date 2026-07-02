package com.fedebacelar.bank.identity.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.identity.application.command.CreateIdentityLinkCommand;
import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.dto.CreateIdentityLinkRequest;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.dto.IdentityLinkResponse;
import org.springframework.stereotype.Component;

@Component
public class IdentityLinkWebMapper {

    public CreateIdentityLinkCommand toCommand(CreateIdentityLinkRequest request) {
        return new CreateIdentityLinkCommand(request.customerId(), request.provider(), request.providerSubject());
    }

    public IdentityLinkResponse toResponse(IdentityLinkDetails details) {
        return new IdentityLinkResponse(
                details.id(),
                details.customerId(),
                details.provider(),
                details.providerSubject(),
                details.status(),
                details.createdAt(),
                details.updatedAt(),
                details.version()
        );
    }
}
