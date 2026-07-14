package com.fedebacelar.bank.onboarding.infrastructure.validation;

import java.net.URI;
import java.util.Locale;

public final class OutboundLinkUriValidator {

    private OutboundLinkUriValidator() {
    }

    public static URI validate(String value, String propertyName, String expectedRawPath) {
        if (value == null || value.isBlank() || value.indexOf('\\') >= 0) {
            throw invalid(propertyName);
        }

        final URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalid(propertyName, exception);
        }

        String scheme = uri.getScheme();
        if (!uri.isAbsolute() || uri.isOpaque() || scheme == null) {
            throw invalid(propertyName);
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw invalid(propertyName);
        }

        String rawAuthority = uri.getRawAuthority();
        String host = uri.getHost();
        if (rawAuthority == null || rawAuthority.indexOf('%') >= 0
                || uri.getRawUserInfo() != null || host == null || host.isBlank()) {
            throw invalid(propertyName);
        }

        String normalizedHost = withoutIpv6Brackets(host).toLowerCase(Locale.ROOT);
        if (normalizedHost.endsWith(".") || ("http".equals(scheme) && !isExplicitLoopback(normalizedHost))) {
            throw invalid(propertyName);
        }

        int port = uri.getPort();
        if (port == 0 || port > 65_535 || !hasCanonicalAuthority(rawAuthority, host, port)) {
            throw invalid(propertyName);
        }

        if (!expectedRawPath.equals(uri.getRawPath())
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw invalid(propertyName);
        }

        return uri;
    }

    private static boolean hasCanonicalAuthority(String rawAuthority, String host, int port) {
        String authorityHost = host;
        if (host.indexOf(':') >= 0 && !(host.startsWith("[") && host.endsWith("]"))) {
            authorityHost = "[" + host + "]";
        }
        String expectedAuthority = port == -1 ? authorityHost : authorityHost + ":" + port;
        return rawAuthority.equalsIgnoreCase(expectedAuthority);
    }

    private static String withoutIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean isExplicitLoopback(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private static IllegalArgumentException invalid(String propertyName) {
        return new IllegalArgumentException(propertyName + " must be a safe absolute HTTP(S) URL.");
    }

    private static IllegalArgumentException invalid(String propertyName, IllegalArgumentException cause) {
        return new IllegalArgumentException(propertyName + " must be a safe absolute HTTP(S) URL.", cause);
    }
}
