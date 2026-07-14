package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkFactoryPort;
import com.fedebacelar.bank.onboarding.infrastructure.validation.OutboundLinkUriValidator;
import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredMagicLinkFactoryAdapter implements MagicLinkFactoryPort {

    private static final String MAGIC_LINK_PATH = "/onboarding/continue";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final URI baseUri;

    public ConfiguredMagicLinkFactoryAdapter(
            @Value("${onboarding.frontend.magic-link-base-url:http://localhost:4200/onboarding/continue}")
            String magicLinkBaseUrl
    ) {
        this.baseUri = OutboundLinkUriValidator.validate(
                magicLinkBaseUrl,
                "onboarding.frontend.magic-link-base-url",
                MAGIC_LINK_PATH
        );
    }

    @Override
    public String create(String token) {
        if (token == null || !TOKEN_PATTERN.matcher(token).matches()) {
            throw new IllegalArgumentException("Magic link token must be a 43-character Base64URL value.");
        }
        return baseUri.toASCIIString() + "#token=" + token;
    }
}
