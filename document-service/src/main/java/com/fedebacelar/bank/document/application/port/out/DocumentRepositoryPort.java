package com.fedebacelar.bank.document.application.port.out;

import com.fedebacelar.bank.document.domain.model.Document;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {

    Document save(Document document);

    Optional<Document> findById(UUID documentId);

    Optional<Document> findByIdempotencyKey(String idempotencyKey);
}

