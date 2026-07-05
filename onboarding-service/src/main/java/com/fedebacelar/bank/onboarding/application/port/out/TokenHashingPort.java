package com.fedebacelar.bank.onboarding.application.port.out;

public interface TokenHashingPort {

    String hash(String token);
}
