package com.fedebacelar.bank.onboarding.domain.exception;

import java.util.Set;

public class OnboardingIncompleteException extends RuntimeException {
    private final Set<String> missingSections;

    public OnboardingIncompleteException(Set<String> missingSections) {
        super("Onboarding application is incomplete");
        this.missingSections = Set.copyOf(missingSections);
    }

    public Set<String> missingSections() {
        return missingSections;
    }
}
