package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EmailActionLinkPolicyTest {
    private static final String TOKEN = "A".repeat(43);
    private static final String MAGIC_LINK =
            "http://localhost:4200/onboarding/continue#token=" + TOKEN;
    private static final String CREDENTIAL_LINK =
            "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token"
                    + "?key=header.payload.signature";

    private final EmailActionLinkPolicy policy = new EmailActionLinkPolicy(
            "http://localhost:4200,https://app.nerva.example",
            "http://localhost:8090,https://identity.nerva.example"
    );

    @Test
    void acceptsOnlyTheRegisteredContractForEachTemplate() {
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                MAGIC_LINK
        )).isTrue();
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                "https://app.nerva.example/onboarding/continue#token=" + TOKEN
        )).isTrue();
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                "credentialSetupLink",
                CREDENTIAL_LINK
        )).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://attacker.example/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4201/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://127.0.0.1:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200/onboarding/continue/#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200/onboarding/continue?next=evil#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200/onboarding/continue#token=short",
            "http://localhost:4200@attacker.example/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "javascript:alert(1)",
            "/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    })
    void rejectsMagicLinksThatDoNotMatchTheExactOriginAndRoute(String link) {
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                link
        )).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token",
            "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token?key=",
            "http://localhost:8090/realms/other/login-actions/action-token?key=header.payload.signature",
            "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token?key=header.payload.signature&redirect_uri=https://attacker.example",
            "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token?key=header.payload.signature#continue"
    })
    void rejectsCredentialLinksOutsideTheKeycloakActionContract(String link) {
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                "credentialSetupLink",
                link
        )).isFalse();
    }

    @Test
    void failsStartupForMalformedOrOverbroadConfiguredOrigins() {
        assertThatThrownBy(() -> new EmailActionLinkPolicy(
                "https://*.nerva.example",
                "http://localhost:8090"
        )).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailActionLinkPolicy(
                "https://app.nerva.example/onboarding",
                "http://localhost:8090"
        )).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailActionLinkPolicy(
                "http://app.nerva.example",
                "http://localhost:8090"
        )).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailActionLinkPolicy(
                "",
                "http://localhost:8090"
        )).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailActionLinkPolicy(
                "http://localhost:4200,",
                "http://localhost:8090"
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void doesNotTreatLinkVariablesAsInterchangeable() {
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "credentialSetupLink",
                MAGIC_LINK
        )).isFalse();
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                "magicLink",
                CREDENTIAL_LINK
        )).isFalse();
    }

    @Test
    void bindsMultipleCommaSeparatedOriginsThroughTheRealSpringConfigurationPath() {
        new ApplicationContextRunner()
                .withBean(EmailActionLinkPolicy.class)
                .withPropertyValues(
                        "notification.action-links.onboarding-allowed-origins="
                                + "http://localhost:4200,https://app.nerva.example",
                        "notification.action-links.keycloak-allowed-origins="
                                + "http://localhost:8090,https://identity.nerva.example"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    EmailActionLinkPolicy configuredPolicy = context.getBean(EmailActionLinkPolicy.class);
                    assertThat(configuredPolicy.allows(
                            NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                            "magicLink",
                            "https://app.nerva.example/onboarding/continue#token=" + TOKEN
                    )).isTrue();
                    assertThat(configuredPolicy.allows(
                            NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                            "credentialSetupLink",
                            "https://identity.nerva.example/realms/banking-ecosystem/"
                                    + "login-actions/action-token?key=header.payload.signature"
                    )).isTrue();
                });
    }

    @Test
    void comparesEffectivePortsAndSupportsOnlyExplicitlyConfiguredIpv6Loopback() {
        EmailActionLinkPolicy ipv6Policy = new EmailActionLinkPolicy(
                "http://[::1]:4200,https://app.nerva.example",
                "http://localhost:8090"
        );

        assertThat(ipv6Policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                "http://[::1]:4200/onboarding/continue#token=" + TOKEN
        )).isTrue();
        assertThat(ipv6Policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                "https://app.nerva.example:443/onboarding/continue#token=" + TOKEN
        )).isTrue();
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                "http://[::1]:4200/onboarding/continue#token=" + TOKEN
        )).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:4200/onboarding/%63ontinue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200/onboarding/../onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost.:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://local%68ost:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200\\@attacker.example/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200/onboarding/continue?#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "http://localhost:4200/onboarding/continue#",
            "http://2130706433:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    })
    void rejectsNonCanonicalAndEncodedMagicLinkVariants(String link) {
        assertThat(policy.allows(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "magicLink",
                link
        )).isFalse();
    }
}
