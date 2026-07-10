package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "document-service")
public interface DocumentFeignClient {
    @GetMapping("/internal/documents/{documentId}")
    DocumentMetadataResponse get(@PathVariable UUID documentId);
}
