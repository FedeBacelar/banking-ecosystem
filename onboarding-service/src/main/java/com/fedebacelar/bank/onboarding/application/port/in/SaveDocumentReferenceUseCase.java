package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.SaveDocumentReferenceCommand;
import com.fedebacelar.bank.onboarding.application.view.DocumentReferenceDetails;

public interface SaveDocumentReferenceUseCase {

    DocumentReferenceDetails save(SaveDocumentReferenceCommand command);
}
