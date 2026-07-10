package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RequestFingerprint {
    private final ObjectMapper objectMapper;
    public RequestFingerprint(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public String hash(Object request) {
        try {
            byte[] body = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not fingerprint idempotent request.", exception);
        }
    }
}
