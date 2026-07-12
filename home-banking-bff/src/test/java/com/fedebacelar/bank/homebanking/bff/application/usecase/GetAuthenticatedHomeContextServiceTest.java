package com.fedebacelar.bank.homebanking.bff.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.port.out.GetCustomerAccountsPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetCustomerPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import com.fedebacelar.bank.homebanking.bff.application.port.out.ResolveIdentityLinkPort;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import com.fedebacelar.bank.homebanking.bff.domain.model.IdentityLink;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GetAuthenticatedHomeContextServiceTest {

    private final ResolveIdentityLinkPort resolveIdentityLinkPort = mock(ResolveIdentityLinkPort.class);
    private final GetCustomerPort getCustomerPort = mock(GetCustomerPort.class);
    private final GetCustomerAccountsPort getCustomerAccountsPort = mock(GetCustomerAccountsPort.class);
    private final GetInternalAccessTokenPort getInternalAccessTokenPort = mock(GetInternalAccessTokenPort.class);
    private final GetAuthenticatedHomeContextService useCase = new GetAuthenticatedHomeContextService(
            resolveIdentityLinkPort,
            getCustomerPort,
            getCustomerAccountsPort,
            getInternalAccessTokenPort
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldComposeAuthenticatedHomeContext() throws Exception {
        UUID customerId = UUID.randomUUID();
        String accessToken = "access-token";
        AuthenticatedUser user = new AuthenticatedUser("keycloak-sub", "homebanking-user", "user@local.dev");
        JsonNode customer = objectMapper.readTree("{\"customerId\":\"%s\"}".formatted(customerId));
        JsonNode account = objectMapper.readTree("{\"accountNumber\":\"ACC-2026-000001\"}");

        when(getInternalAccessTokenPort.getAccessToken(InternalAccessPurpose.HOME_BANKING)).thenReturn(accessToken);
        when(resolveIdentityLinkPort.resolveByKeycloakSubject(user.subject(), accessToken))
                .thenReturn(new IdentityLink(customerId));
        when(getCustomerPort.getCustomer(customerId, accessToken)).thenReturn(customer);
        when(getCustomerAccountsPort.getAccounts(customerId, accessToken)).thenReturn(List.of(account));

        HomeBankingContext context = useCase.getHomeContext(user);

        assertThat(context.subject()).isEqualTo(user.subject());
        assertThat(context.username()).isEqualTo(user.username());
        assertThat(context.email()).isEqualTo(user.email());
        assertThat(context.customerId()).isEqualTo(customerId);
        assertThat(context.customer()).isEqualTo(customer);
        assertThat(context.accounts()).containsExactly(account);
    }
}
