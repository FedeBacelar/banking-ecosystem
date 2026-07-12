package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bridges the application-level upload abstraction to Feign's supported multipart contract.
 */
final class OnboardingMultipartFile implements MultipartFile {

    private static final String PART_NAME = "file";

    private final OnboardingDocumentUpload document;

    OnboardingMultipartFile(OnboardingDocumentUpload document) {
        this.document = document;
    }

    @Override
    public String getName() {
        return PART_NAME;
    }

    @Override
    public String getOriginalFilename() {
        return document.originalFilename();
    }

    @Override
    public String getContentType() {
        return document.contentType();
    }

    @Override
    public boolean isEmpty() {
        return document.size() == 0;
    }

    @Override
    public long getSize() {
        return document.size();
    }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream inputStream = document.openStream()) {
            return inputStream.readAllBytes();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return document.openStream();
    }

    @Override
    public void transferTo(File destination) throws IOException, IllegalStateException {
        try (InputStream inputStream = document.openStream()) {
            Files.copy(inputStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
