package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.io.IOException;
import java.io.InputStream;

public interface OnboardingFile {

    String originalFilename();

    String contentType();

    long size();

    InputStream openStream() throws IOException;
}
