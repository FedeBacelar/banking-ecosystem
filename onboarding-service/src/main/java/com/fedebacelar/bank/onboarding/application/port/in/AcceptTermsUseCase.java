package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.AcceptTermsCommand;
import com.fedebacelar.bank.onboarding.application.view.TermsAcceptanceDetails;

public interface AcceptTermsUseCase {

    TermsAcceptanceDetails accept(AcceptTermsCommand command);
}
