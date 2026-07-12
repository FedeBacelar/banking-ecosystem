package com.fedebacelar.bank.homebanking.bff.application.port.out;

public interface GetInternalAccessTokenPort {

    String getAccessToken(InternalAccessPurpose purpose);
}
