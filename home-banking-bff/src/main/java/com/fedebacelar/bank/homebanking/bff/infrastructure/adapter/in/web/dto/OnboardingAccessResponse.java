package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingNextAction;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;

public record OnboardingAccessResponse(
        OnboardingState status,
        OnboardingNextAction nextAction
) {

    public static OnboardingAccessResponse from(OnboardingState status) {
        return new OnboardingAccessResponse(status, status.nextAction());
    }
}
