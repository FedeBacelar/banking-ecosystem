package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingFile;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

final class MultipartOnboardingFile implements OnboardingFile {

    private final MultipartFile delegate;

    MultipartOnboardingFile(MultipartFile delegate) {
        this.delegate = delegate;
    }

    @Override
    public String originalFilename() {
        String filename = delegate.getOriginalFilename();
        return filename == null || filename.isBlank() ? "document" : filename;
    }

    @Override
    public String contentType() {
        String contentType = delegate.getContentType();
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    @Override
    public long size() {
        return delegate.getSize();
    }

    @Override
    public InputStream openStream() throws IOException {
        return delegate.getInputStream();
    }
}
