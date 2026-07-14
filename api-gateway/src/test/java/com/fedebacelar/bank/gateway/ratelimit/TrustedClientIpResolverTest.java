package com.fedebacelar.bank.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class TrustedClientIpResolverTest {

    @Test
    void shouldIgnoreForwardedHeadersFromAnUntrustedPeer() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("");

        String resolved = resolver.resolve(request("203.0.113.5")
                .header("X-Forwarded-For", "198.51.100.10")
                .header("Forwarded", "for=198.51.100.11")
                .build());

        assertThat(resolved).isEqualTo("ipv4:cb007105");
    }

    @Test
    void shouldWalkATrustedProxyChainFromRightToLeft() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("10.0.0.0/8, 192.168.0.0/16");

        String resolved = resolver.resolve(request("10.9.8.7")
                .header("X-Forwarded-For", "198.51.100.7, 192.168.10.2")
                .build());

        assertThat(resolved).isEqualTo("ipv4:c6336407");
    }

    @Test
    void shouldUseTheLeftmostAddressWhenTheEntireChainIsTrusted() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("10.0.0.0/8");

        String resolved = resolver.resolve(request("10.9.8.7")
                .header("X-Forwarded-For", "10.1.1.1, 10.2.2.2")
                .build());

        assertThat(resolved).isEqualTo("ipv4:0a010101");
    }

    @Test
    void shouldIgnoreAClientSuppliedPrefixBeforeTheFirstUntrustedHop() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("10.0.0.0/8");

        String resolved = resolver.resolve(request("10.9.8.7")
                .header("X-Forwarded-For", "203.0.113.99, 198.51.100.7, 10.1.1.1")
                .build());

        assertThat(resolved).isEqualTo("ipv4:c6336407");
    }

    @Test
    void shouldFallBackToTheTrustedPeerWhenTheForwardedChainIsMalformed() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("10.0.0.0/8");

        String resolved = resolver.resolve(request("10.9.8.7")
                .header("X-Forwarded-For", "198.51.100.7, attacker.example")
                .build());

        assertThat(resolved).isEqualTo("ipv4:0a090807");
    }

    @Test
    void shouldCanonicalizeEquivalentIpv6Addresses() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("");

        String compressed = resolver.resolve(request("2001:db8::1").build());
        String expanded = resolver.resolve(request("2001:0db8:0:0:0:0:0:1").build());

        assertThat(compressed).isEqualTo(expanded).startsWith("ipv6:");
    }

    @Test
    void shouldGroupIpv6PrivacyAddressesByNetworkPrefix() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("", 64);

        String first = resolver.resolve(request("2001:db8:1234:5678::1").build());
        String sameNetwork = resolver.resolve(request("2001:db8:1234:5678:ffff::99").build());
        String otherNetwork = resolver.resolve(request("2001:db8:1234:5679::1").build());

        assertThat(first).isEqualTo(sameNetwork);
        assertThat(first).isNotEqualTo(otherNetwork);
    }

    @Test
    void shouldNormalizeIpv4MappedIpv6AsIpv4() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("");

        String ipv4 = resolver.resolve(request("192.0.2.1").build());
        String mapped = resolver.resolve(request("::ffff:192.0.2.1").build());

        assertThat(mapped).isEqualTo(ipv4).isEqualTo("ipv4:c0000201");
    }

    @Test
    void shouldMatchTrustedCidrsWithNonByteAlignedPrefixes() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("192.0.2.128/25");

        String trusted = resolver.resolve(request("192.0.2.200")
                .header("X-Forwarded-For", "198.51.100.10")
                .build());
        String untrusted = resolver.resolve(request("192.0.2.100")
                .header("X-Forwarded-For", "198.51.100.10")
                .build());

        assertThat(trusted).isEqualTo("ipv4:c633640a");
        assertThat(untrusted).isEqualTo("ipv4:c0000264");
    }

    @Test
    void shouldFailClosedWhenTheSocketAddressIsUnavailable() {
        TrustedClientIpResolver resolver = new TrustedClientIpResolver("");

        String resolved = resolver.resolve(MockServerHttpRequest.post("/web/onboarding/applications").build());

        assertThat(resolved).isEqualTo(TrustedClientIpResolver.UNKNOWN_CLIENT);
    }

    @Test
    void shouldRejectInvalidTrustedProxyConfigurationWithoutEchoingIt() {
        assertThatThrownBy(() -> new TrustedClientIpResolver("proxy.internal/24"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trusted-proxies must contain only valid IP CIDRs")
                .hasMessageNotContaining("proxy.internal");
    }

    @Test
    void shouldRejectTrustingTheEntireInternet() {
        assertThatThrownBy(() -> new TrustedClientIpResolver("0.0.0.0/0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trusted-proxies must contain only valid IP CIDRs");
        assertThatThrownBy(() -> new TrustedClientIpResolver("::/0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trusted-proxies must contain only valid IP CIDRs");
    }

    private MockServerHttpRequest.BaseBuilder<?> request(String remoteAddress) {
        return MockServerHttpRequest.post("/web/onboarding/applications")
                .remoteAddress(new InetSocketAddress(remoteAddress, 54_321));
    }
}
