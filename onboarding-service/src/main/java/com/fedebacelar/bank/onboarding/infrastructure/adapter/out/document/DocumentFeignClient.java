package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "document-service")
public interface DocumentFeignClient {
    @PostMapping(value = "/internal/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DocumentMetadataResponse upload(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Content-SHA256") String contentSha256,
            @RequestParam String businessContext,
            @RequestParam String businessReferenceId,
            @RequestParam String category,
            @RequestPart("file") MultipartFile file
    );

    @GetMapping("/internal/documents/{documentId}")
    DocumentMetadataResponse get(@PathVariable UUID documentId);
}
