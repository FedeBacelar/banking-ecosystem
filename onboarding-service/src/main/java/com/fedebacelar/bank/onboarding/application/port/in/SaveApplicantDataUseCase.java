package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.SaveApplicantDataCommand;
import com.fedebacelar.bank.onboarding.application.view.ApplicantDataDetails;

public interface SaveApplicantDataUseCase {

    ApplicantDataDetails save(SaveApplicantDataCommand command);
}
