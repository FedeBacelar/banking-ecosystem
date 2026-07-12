package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import org.springframework.web.multipart.MultipartFile;

final class MultipartDocumentUpload {

    private MultipartDocumentUpload() {
    }

    static OnboardingDocumentUpload from(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        return new OnboardingDocumentUpload(
                filename == null || filename.isBlank() ? "document" : filename,
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType,
                file.getSize(),
                file::getInputStream
        );
    }
}
