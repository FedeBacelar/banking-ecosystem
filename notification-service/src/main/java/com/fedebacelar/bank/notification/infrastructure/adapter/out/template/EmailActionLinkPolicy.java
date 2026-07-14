package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailActionLinkPolicy {
    private static final String MAGIC_LINK_VARIABLE = "magicLink";
    private static final String CREDENTIAL_LINK_VARIABLE = "credentialSetupLink";
    private static final String MAGIC_LINK_PATH = "/onboarding/continue";
    private static final String CREDENTIAL_LINK_PATH =
            "/realms/banking-ecosystem/login-actions/action-token";
    private static final Pattern MAGIC_LINK_FRAGMENT = Pattern.compile("token=[A-Za-z0-9_-]{43}");
    private static final Pattern CREDENTIAL_LINK_QUERY = Pattern.compile("key=[A-Za-z0-9._~-]+");
    private static final Pattern ASCII_HOST = Pattern.compile("[A-Za-z0-9.-]+|\\[[0-9A-Fa-f:]+]");

    private final Set<Origin> onboardingOrigins;
    private final Set<Origin> keycloakOrigins;

    public EmailActionLinkPolicy(
            @Value("${notification.action-links.onboarding-allowed-origins}")
            String onboardingAllowedOrigins,
            @Value("${notification.action-links.keycloak-allowed-origins}")
            String keycloakAllowedOrigins
    ) {
        this.onboardingOrigins = configuredOrigins(onboardingAllowedOrigins);
        this.keycloakOrigins = configuredOrigins(keycloakAllowedOrigins);
    }

    public boolean allows(
            NotificationTemplateCode templateCode,
            String variableName,
            String value
    ) {
        ParsedLink link = parseLink(value);
        if (link == null) {
            return false;
        }
        return switch (templateCode) {
            case ONBOARDING_EMAIL_MAGIC_LINK -> MAGIC_LINK_VARIABLE.equals(variableName)
                    && onboardingOrigins.contains(link.origin())
                    && MAGIC_LINK_PATH.equals(link.rawPath())
                    && link.rawQuery() == null
                    && link.rawFragment() != null
                    && MAGIC_LINK_FRAGMENT.matcher(link.rawFragment()).matches();
            case ONBOARDING_APPROVED_CREDENTIAL_INVITATION ->
                    CREDENTIAL_LINK_VARIABLE.equals(variableName)
                            && keycloakOrigins.contains(link.origin())
                            && CREDENTIAL_LINK_PATH.equals(link.rawPath())
                            && link.rawQuery() != null
                            && CREDENTIAL_LINK_QUERY.matcher(link.rawQuery()).matches()
                            && link.rawFragment() == null;
            case ONBOARDING_REJECTED, ONBOARDING_COMPLETED -> false;
        };
    }

    private Set<Origin> configuredOrigins(String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            throw invalidConfiguration();
        }
        Set<Origin> origins = new HashSet<>();
        for (String rawValue : configuredValue.split(",", -1)) {
            String value = rawValue.trim();
            if (value.isEmpty()) {
                throw invalidConfiguration();
            }
            try {
                URI uri = URI.create(value);
                if (uri.getRawQuery() != null
                        || uri.getRawFragment() != null
                        || (uri.getRawPath() != null
                        && !uri.getRawPath().isEmpty()
                        && !"/".equals(uri.getRawPath()))) {
                    throw invalidConfiguration();
                }
                origins.add(origin(uri, true));
            } catch (IllegalArgumentException exception) {
                throw invalidConfiguration();
            }
        }
        if (origins.isEmpty()) {
            throw invalidConfiguration();
        }
        return Set.copyOf(origins);
    }

    private ParsedLink parseLink(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            Origin origin = origin(uri, false);
            String rawPath = uri.getRawPath();
            if (rawPath == null || !rawPath.equals(uri.normalize().getRawPath())) {
                return null;
            }
            return new ParsedLink(origin, rawPath, uri.getRawQuery(), uri.getRawFragment());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Origin origin(URI uri, boolean configuredOrigin) {
        String rawAuthority = uri.getRawAuthority();
        String host = uri.getHost();
        if (!uri.isAbsolute()
                || uri.isOpaque()
                || rawAuthority == null
                || rawAuthority.contains("\\")
                || rawAuthority.contains("%")
                || uri.getUserInfo() != null
                || host == null
                || !ASCII_HOST.matcher(host).matches()
                || host.endsWith(".")) {
            throw configuredOrigin ? invalidConfiguration() : new IllegalArgumentException();
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            throw configuredOrigin ? invalidConfiguration() : new IllegalArgumentException();
        }
        if ("http".equals(scheme) && !isLoopbackHost(host)) {
            throw configuredOrigin ? invalidConfiguration() : new IllegalArgumentException();
        }
        int explicitPort = uri.getPort();
        if (explicitPort == 0 || explicitPort > 65_535) {
            throw configuredOrigin ? invalidConfiguration() : new IllegalArgumentException();
        }
        int effectivePort = explicitPort == -1 ? ("https".equals(scheme) ? 443 : 80) : explicitPort;
        return new Origin(scheme, host.toLowerCase(Locale.ROOT), effectivePort);
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private IllegalStateException invalidConfiguration() {
        return new IllegalStateException(
                "Email action-link origins must contain exact HTTP loopback or HTTPS origins only."
        );
    }

    private record Origin(String scheme, String host, int port) {
    }

    private record ParsedLink(
            Origin origin,
            String rawPath,
            String rawQuery,
            String rawFragment
    ) {
    }
}
