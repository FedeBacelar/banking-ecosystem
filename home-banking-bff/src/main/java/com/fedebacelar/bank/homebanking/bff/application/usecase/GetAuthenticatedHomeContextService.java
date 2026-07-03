package com.fedebacelar.bank.homebanking.bff.application.usecase;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetCustomerAccountsPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetCustomerPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.ResolveIdentityLinkPort;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import com.fedebacelar.bank.homebanking.bff.domain.model.IdentityLink;
import org.springframework.stereotype.Service;

@Service
public class GetAuthenticatedHomeContextService implements GetAuthenticatedHomeContextUseCase {

    private final ResolveIdentityLinkPort resolveIdentityLinkPort;
    private final GetCustomerPort getCustomerPort;
    private final GetCustomerAccountsPort getCustomerAccountsPort;

    public GetAuthenticatedHomeContextService(
            ResolveIdentityLinkPort resolveIdentityLinkPort,
            GetCustomerPort getCustomerPort,
            GetCustomerAccountsPort getCustomerAccountsPort
    ) {
        this.resolveIdentityLinkPort = resolveIdentityLinkPort;
        this.getCustomerPort = getCustomerPort;
        this.getCustomerAccountsPort = getCustomerAccountsPort;
    }

    @Override
    public HomeBankingContext getHomeContext(AuthenticatedUser user, String accessToken) {
        IdentityLink identityLink = resolveIdentityLinkPort.resolveByKeycloakSubject(user.subject(), accessToken);

        return new HomeBankingContext(
                user.subject(),
                user.username(),
                user.email(),
                identityLink.customerId(),
                getCustomerPort.getCustomer(identityLink.customerId(), accessToken),
                getCustomerAccountsPort.getAccounts(identityLink.customerId(), accessToken)
        );
    }
}
