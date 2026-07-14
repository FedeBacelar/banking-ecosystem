package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfiguredMagicLinkFactoryAdapterTest {

    private static final String TOKEN = "A".repeat(41) + "_-";

    @Test
    void createsTheTokenOnlyInTheFragmentForAValidBaseUrl() {
        var factory = new ConfiguredMagicLinkFactoryAdapter(
                "http://localhost:4200/onboarding/continue"
        );

        assertThat(factory.create(TOKEN)).isEqualTo(
                "http://localhost:4200/onboarding/continue#token=" + TOKEN
        );
    }

    @Test
    void acceptsHttpsAndOnlyExplicitLoopbackHostsForHttp() {
        assertThat(new ConfiguredMagicLinkFactoryAdapter(
                "https://app.nerva.example/onboarding/continue"
        ).create(TOKEN)).startsWith("https://app.nerva.example/");
        assertThat(new ConfiguredMagicLinkFactoryAdapter(
                "http://127.0.0.1:4200/onboarding/continue"
        ).create(TOKEN)).startsWith("http://127.0.0.1:4200/");
        assertThat(new ConfiguredMagicLinkFactoryAdapter(
                "http://[::1]:4200/onboarding/continue"
        ).create(TOKEN)).startsWith("http://[::1]:4200/");
    }

    @Test
    void rejectsUnsafeOrNonCanonicalBaseUrls() {
        List<String> invalidBaseUrls = List.of(
                "/onboarding/continue",
                "ftp://localhost:4200/onboarding/continue",
                "http://app.nerva.example/onboarding/continue",
                "http://user@localhost:4200/onboarding/continue",
                "http://localhost:4200/onboarding/continue?source=email",
                "http://localhost:4200/onboarding/continue#existing",
                "http://localhost:4200/onboarding/continue/",
                "http://localhost:4200/onboarding/%63ontinue",
                "http://localhost:4200/onboarding/../onboarding/continue",
                "http://localhost.:4200/onboarding/continue",
                "http://local%68ost:4200/onboarding/continue",
                "http://localhost%3A4200/onboarding/continue",
                "http://localhost:4200\\@attacker.example/onboarding/continue",
                "http://localhost:0/onboarding/continue",
                "http://localhost:65536/onboarding/continue",
                "http://localhost:not-a-port/onboarding/continue"
        );

        invalidBaseUrls.forEach(baseUrl -> assertThatThrownBy(
                () -> new ConfiguredMagicLinkFactoryAdapter(baseUrl)
        ).as("base URL %s", baseUrl)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("onboarding.frontend.magic-link-base-url"));
    }

    @Test
    void rejectsTokensThatAreNotExactlyA43CharacterBase64UrlValue() {
        var factory = new ConfiguredMagicLinkFactoryAdapter(
                "http://localhost:4200/onboarding/continue"
        );

        List.of("", "A".repeat(42), "A".repeat(44), "A".repeat(42) + "+")
                .forEach(token -> assertThatThrownBy(() -> factory.create(token))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Magic link token must be a 43-character Base64URL value."));
        assertThatThrownBy(() -> factory.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Magic link token must be a 43-character Base64URL value.");
    }
}
