package com.fedebacelar.bank.document.domain.model;

import java.io.InputStream;

public record DocumentFile(
        InputStream content,
        long size,
        String contentType,
        String originalFilename
) {
}

