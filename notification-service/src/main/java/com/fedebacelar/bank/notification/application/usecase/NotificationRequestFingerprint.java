package com.fedebacelar.bank.notification.application.usecase;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

final class NotificationRequestFingerprint {

    private NotificationRequestFingerprint() {
    }

    static String calculate(
            String recipient,
            NotificationTemplateCode templateCode,
            Map<String, String> variables
    ) {
        MessageDigest digest = sha256();
        update(digest, recipient);
        update(digest, templateCode.name());

        TreeMap<String, String> orderedVariables = new TreeMap<>(variables);
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(orderedVariables.size())
                .array());
        orderedVariables.forEach((key, value) -> {
            update(digest, key);
            update(digest, value);
        });

        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
