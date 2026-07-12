package com.fedebacelar.bank.onboarding.application.command;

import java.io.IOException;
import java.io.InputStream;

public record OnboardingDocumentUpload(
        String originalFilename,
        String contentType,
        long size,
        ContentSource contentSource
) {
    public InputStream openStream() throws IOException {
        return contentSource.openStream();
    }

    @FunctionalInterface
    public interface ContentSource {
        InputStream openStream() throws IOException;
    }
}
