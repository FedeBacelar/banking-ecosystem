package com.fedebacelar.bank.document.infrastructure.adapter.in.web;

import com.fedebacelar.bank.document.application.port.in.GetDocumentUseCase;
import com.fedebacelar.bank.document.application.port.in.UploadDocumentUseCase;
import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.infrastructure.adapter.in.web.dto.DocumentResponse;
import com.fedebacelar.bank.document.infrastructure.adapter.in.web.mapper.DocumentWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/internal/documents")
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final DocumentWebMapper mapper;

    public DocumentController(
            UploadDocumentUseCase uploadDocumentUseCase,
            GetDocumentUseCase getDocumentUseCase,
            DocumentWebMapper mapper
    ) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Upload internal business document")
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Z_]+$") String businessContext,
            @RequestParam @NotBlank @Size(max = 120) String businessReferenceId,
            @RequestParam @NotNull DocumentCategory category,
            @RequestParam @NotNull MultipartFile file
    ) {
        return mapper.toResponse(uploadDocumentUseCase.upload(
                mapper.toCommand(businessContext, businessReferenceId, category, file)
        ));
    }

    @Operation(summary = "Get document metadata")
    @GetMapping("/{documentId}")
    public DocumentResponse get(@PathVariable UUID documentId) {
        return mapper.toResponse(getDocumentUseCase.get(documentId));
    }
}
