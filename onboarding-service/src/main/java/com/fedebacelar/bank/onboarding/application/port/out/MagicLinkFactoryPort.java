package com.fedebacelar.bank.onboarding.application.port.out;

public interface MagicLinkFactoryPort {

    String create(String token);
}
