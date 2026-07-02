package com.fedebacelar.bank.identity.application.usecase;

import com.fedebacelar.bank.identity.application.command.CreateIdentityLinkCommand;
import com.fedebacelar.bank.identity.application.mapper.IdentityLinkDetailsMapper;
import com.fedebacelar.bank.identity.application.port.in.ChangeIdentityLinkStatusUseCase;
import com.fedebacelar.bank.identity.application.port.in.CreateIdentityLinkUseCase;
import com.fedebacelar.bank.identity.application.port.in.GetCustomerIdentityLinksUseCase;
import com.fedebacelar.bank.identity.application.port.in.ResolveIdentityLinkUseCase;
import com.fedebacelar.bank.identity.application.port.out.IdentityLinkRepositoryPort;
import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.exception.DuplicateIdentityLinkException;
import com.fedebacelar.bank.identity.domain.exception.IdentityLinkNotFoundException;
import com.fedebacelar.bank.identity.domain.exception.InactiveIdentityLinkException;
import com.fedebacelar.bank.identity.domain.model.IdentityLink;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityLinkService implements CreateIdentityLinkUseCase, ResolveIdentityLinkUseCase, GetCustomerIdentityLinksUseCase, ChangeIdentityLinkStatusUseCase {

    private final IdentityLinkRepositoryPort repositoryPort;
    private final Clock clock;

    public IdentityLinkService(IdentityLinkRepositoryPort repositoryPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IdentityLinkDetails create(CreateIdentityLinkCommand command) {
        if (repositoryPort.existsByProviderAndProviderSubject(command.provider(), command.providerSubject())) {
            throw new DuplicateIdentityLinkException(command.provider(), command.providerSubject());
        }

        IdentityLink identityLink = IdentityLink.create(
                command.customerId(),
                command.provider(),
                command.providerSubject(),
                Instant.now(clock)
        );

        return IdentityLinkDetailsMapper.toDetails(repositoryPort.save(identityLink));
    }

    @Override
    @Transactional(readOnly = true)
    public IdentityLinkDetails resolveActive(IdentityProvider provider, String providerSubject) {
        IdentityLink identityLink = repositoryPort.findByProviderAndProviderSubject(provider, providerSubject)
                .orElseThrow(() -> new IdentityLinkNotFoundException(provider, providerSubject));

        if (identityLink.status() != IdentityLinkStatus.ACTIVE) {
            throw new InactiveIdentityLinkException(provider, providerSubject, identityLink.status());
        }

        return IdentityLinkDetailsMapper.toDetails(identityLink);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityLinkDetails> getByCustomerId(UUID customerId) {
        return repositoryPort.findByCustomerId(customerId).stream()
                .map(IdentityLinkDetailsMapper::toDetails)
                .toList();
    }

    @Override
    @Transactional
    public IdentityLinkDetails activate(UUID identityLinkId) {
        IdentityLink identityLink = repositoryPort.findById(identityLinkId)
                .orElseThrow(() -> new IdentityLinkNotFoundException(identityLinkId));
        return IdentityLinkDetailsMapper.toDetails(repositoryPort.save(identityLink.activate(Instant.now(clock))));
    }

    @Override
    @Transactional
    public IdentityLinkDetails disable(UUID identityLinkId) {
        IdentityLink identityLink = repositoryPort.findById(identityLinkId)
                .orElseThrow(() -> new IdentityLinkNotFoundException(identityLinkId));
        return IdentityLinkDetailsMapper.toDetails(repositoryPort.save(identityLink.disable(Instant.now(clock))));
    }
}
