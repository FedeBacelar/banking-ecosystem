package com.fedebacelar.bank.gateway.ratelimit;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

final class TrustedClientIpResolver {

    static final String UNKNOWN_CLIENT = "unknown";

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final int MAX_FORWARDED_CHAIN_LENGTH = 32;
    private static final int MAX_FORWARDED_HEADER_LENGTH = 2_048;
    private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9A-Fa-f:.]+");

    private final List<IpSubnet> trustedProxies;
    private final int clientIpv6PrefixLength;

    TrustedClientIpResolver(String trustedProxyCidrs) {
        this(trustedProxyCidrs, 64);
    }

    TrustedClientIpResolver(String trustedProxyCidrs, int clientIpv6PrefixLength) {
        if (clientIpv6PrefixLength < 48 || clientIpv6PrefixLength > 128) {
            throw new IllegalArgumentException("client IPv6 prefix length is outside the supported range");
        }
        this.trustedProxies = parseTrustedProxies(trustedProxyCidrs);
        this.clientIpv6PrefixLength = clientIpv6PrefixLength;
    }

    String resolve(ServerHttpRequest request) {
        InetAddress peer = socketAddress(request.getRemoteAddress());
        if (peer == null) {
            return UNKNOWN_CLIENT;
        }
        if (!isTrusted(peer)) {
            return clientKey(peer);
        }

        List<InetAddress> forwardedChain = forwardedChain(request.getHeaders());
        if (forwardedChain == null || forwardedChain.isEmpty()) {
            return clientKey(peer);
        }

        InetAddress candidate = peer;
        for (int index = forwardedChain.size() - 1; index >= 0 && isTrusted(candidate); index--) {
            candidate = forwardedChain.get(index);
        }
        return clientKey(candidate);
    }

    private InetAddress socketAddress(InetSocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }
        InetAddress address = socketAddress.getAddress();
        return address != null ? address : parseLiteral(socketAddress.getHostString());
    }

    private List<InetAddress> forwardedChain(HttpHeaders headers) {
        List<String> values = headers.get(X_FORWARDED_FOR);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        String combined = String.join(",", values);
        if (combined.length() > MAX_FORWARDED_HEADER_LENGTH) {
            return null;
        }

        String[] parts = combined.split(",", -1);
        if (parts.length > MAX_FORWARDED_CHAIN_LENGTH) {
            return null;
        }
        List<InetAddress> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            InetAddress address = parseLiteral(part.trim());
            if (address == null) {
                return null;
            }
            result.add(address);
        }
        return List.copyOf(result);
    }

    private boolean isTrusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(subnet -> subnet.contains(address));
    }

    private List<IpSubnet> parseTrustedProxies(String configuredCidrs) {
        if (configuredCidrs == null || configuredCidrs.isBlank()) {
            return List.of();
        }
        String[] values = configuredCidrs.split(",", -1);
        if (values.length > 64) {
            throw invalidTrustedProxyConfiguration();
        }
        List<IpSubnet> result = new ArrayList<>(values.length);
        for (String value : values) {
            result.add(IpSubnet.parse(value.trim()));
        }
        return List.copyOf(result);
    }

    private String clientKey(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            bytes = IpSubnet.mask(bytes, clientIpv6PrefixLength);
        }
        String family = bytes.length == 4 ? "ipv4:" : "ipv6:";
        return family + HexFormat.of().formatHex(bytes);
    }

    private static InetAddress parseLiteral(String value) {
        if (value == null || value.isBlank() || value.length() > 64 || value.indexOf('%') >= 0) {
            return null;
        }
        if (value.indexOf(':') >= 0) {
            if (!IPV6_LITERAL.matcher(value).matches()) {
                return null;
            }
            try {
                return InetAddress.getByName(value);
            } catch (UnknownHostException exception) {
                return null;
            }
        }

        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int index = 0; index < octets.length; index++) {
            String octet = octets[index];
            if (octet.isEmpty() || octet.length() > 3
                    || !octet.chars().allMatch(character -> character >= '0' && character <= '9')) {
                return null;
            }
            int parsed;
            try {
                parsed = Integer.parseInt(octet);
            } catch (NumberFormatException exception) {
                return null;
            }
            if (parsed > 255) {
                return null;
            }
            bytes[index] = (byte) parsed;
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static IllegalArgumentException invalidTrustedProxyConfiguration() {
        return new IllegalArgumentException("trusted-proxies must contain only valid IP CIDRs");
    }

    private record IpSubnet(byte[] network, int prefixLength) {

        private static IpSubnet parse(String value) {
            if (value == null || value.isBlank()) {
                throw invalidTrustedProxyConfiguration();
            }
            int separator = value.indexOf('/');
            if (separator != value.lastIndexOf('/')) {
                throw invalidTrustedProxyConfiguration();
            }
            String addressValue = separator < 0 ? value : value.substring(0, separator);
            InetAddress address = parseLiteral(addressValue);
            if (address == null) {
                throw invalidTrustedProxyConfiguration();
            }
            int bitCount = address.getAddress().length * Byte.SIZE;
            int prefix = bitCount;
            if (separator >= 0) {
                String prefixValue = value.substring(separator + 1);
                try {
                    prefix = Integer.parseInt(prefixValue);
                } catch (NumberFormatException exception) {
                    throw invalidTrustedProxyConfiguration();
                }
            }
            if (prefix <= 0 || prefix > bitCount) {
                throw invalidTrustedProxyConfiguration();
            }
            return new IpSubnet(mask(address.getAddress(), prefix), prefix);
        }

        private boolean contains(InetAddress candidate) {
            byte[] candidateBytes = candidate.getAddress();
            return candidateBytes.length == network.length
                    && Arrays.equals(network, mask(candidateBytes, prefixLength));
        }

        private static byte[] mask(byte[] source, int prefixLength) {
            byte[] result = Arrays.copyOf(source, source.length);
            int fullBytes = prefixLength / Byte.SIZE;
            int remainingBits = prefixLength % Byte.SIZE;
            if (fullBytes < result.length && remainingBits > 0) {
                int mask = 0xFF << (Byte.SIZE - remainingBits);
                result[fullBytes] = (byte) (result[fullBytes] & mask);
                fullBytes++;
            }
            Arrays.fill(result, fullBytes, result.length, (byte) 0);
            return result;
        }
    }
}
