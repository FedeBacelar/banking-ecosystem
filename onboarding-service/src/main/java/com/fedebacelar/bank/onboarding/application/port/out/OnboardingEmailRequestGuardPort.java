package com.fedebacelar.bank.onboarding.application.port.out;

import java.time.Duration;
import java.time.Instant;

public interface OnboardingEmailRequestGuardPort {
    boolean acquireAndRegister(String email, Instant now, Duration cooldown);
}
