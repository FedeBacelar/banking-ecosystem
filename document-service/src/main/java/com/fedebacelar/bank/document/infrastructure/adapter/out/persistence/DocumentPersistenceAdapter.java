package com.fedebacelar.bank.document.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.document.application.port.out.DocumentRepositoryPort;
import com.fedebacelar.bank.document.domain.model.Document;
import com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.mapper.DocumentPersistenceMapper;
import com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.repository.DocumentJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceAdapter implements DocumentRepositoryPort {

    private final DocumentJpaRepository repository;
    private final DocumentPersistenceMapper mapper;

    public DocumentPersistenceAdapter(DocumentJpaRepository repository, DocumentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        return mapper.toDomain(repository.save(mapper.toEntity(document)));
    }

    @Override
    public Optional<Document> findById(UUID documentId) {
        return repository.findById(documentId.toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<Document> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }
}

