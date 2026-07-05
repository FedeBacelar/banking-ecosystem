package com.fedebacelar.bank.document.application.port.in;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.view.DocumentDetails;

public interface UploadDocumentUseCase {

    DocumentDetails upload(UploadDocumentCommand command);
}

