package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;

public interface GetAuthenticatedHomeContextUseCase {

    HomeBankingContext getHomeContext(AuthenticatedUser user, String accessToken);
}
