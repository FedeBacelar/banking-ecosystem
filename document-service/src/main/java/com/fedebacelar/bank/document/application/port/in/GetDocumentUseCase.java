package com.fedebacelar.bank.document.application.port.in;

import com.fedebacelar.bank.document.application.view.DocumentDetails;
import java.util.UUID;

public interface GetDocumentUseCase {

    DocumentDetails get(UUID documentId);
}

