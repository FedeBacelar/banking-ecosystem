package com.fedebacelar.bank.onboarding.application.port.out;

public interface PayloadCipherPort {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
