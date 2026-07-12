package com.fedebacelar.bank.document.domain.model;

import java.io.IOException;
import java.io.InputStream;

public record DocumentFile(
        ContentSource contentSource,
        long size,
        String contentType,
        String originalFilename
) {
    public InputStream openStream() throws IOException {
        return contentSource.openStream();
    }

    @FunctionalInterface
    public interface ContentSource {
        InputStream openStream() throws IOException;
    }
}
