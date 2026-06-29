package com.fedebacelar.bank.account.application.port.out;

import java.util.UUID;

public interface AccountAliasLookupPort {

    boolean existsAlias(String alias);

    boolean existsAliasForOtherAccount(String alias, UUID accountId);
}
