package com.fedebacelar.bank.document.application.command;

import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.model.DocumentFile;

public record UploadDocumentCommand(
        String idempotencyKey,
        String contentSha256,
        String businessContext,
        String businessReferenceId,
        DocumentCategory category,
        DocumentFile file
) {
}

