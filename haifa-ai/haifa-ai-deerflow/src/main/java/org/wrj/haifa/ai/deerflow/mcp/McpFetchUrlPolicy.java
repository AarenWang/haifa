package org.wrj.haifa.ai.deerflow.mcp;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Client-side fail-closed preflight for optional WEB_FETCH MCP tools. */
@Component
public class McpFetchUrlPolicy {

    @FunctionalInterface
    public interface AddressResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final AddressResolver resolver;

    public McpFetchUrlPolicy() {
        this(InetAddress::getAllByName);
    }

    McpFetchUrlPolicy(AddressResolver resolver) {
        this.resolver = resolver;
    }

    public Validation validate(Map<String, Object> arguments) {
        Object raw = arguments.get("url");
        if (raw == null) raw = arguments.get("uri");
        if (!(raw instanceof String value) || !StringUtils.hasText(value)) {
            return Validation.denied("Fetch arguments must contain a non-empty url");
        }
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) return Validation.denied("Fetch only permits HTTPS");
            if (uri.getUserInfo() != null || uri.getFragment() != null) {
                return Validation.denied("Fetch URL userinfo and fragments are forbidden");
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host) || value.contains("%") || !host.chars().allMatch(ch -> ch < 128)) {
                return Validation.denied("Fetch URL host is invalid or encoded");
            }
            String asciiHost = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES);
            if (!asciiHost.equalsIgnoreCase(host) || isSpecialHostname(asciiHost)) {
                return Validation.denied("Fetch URL host is not a public canonical hostname");
            }
            InetAddress[] addresses = resolver.resolve(asciiHost);
            if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(McpFetchUrlPolicy::isForbiddenAddress)) {
                return Validation.denied("Fetch URL resolves to a non-public address");
            }
            int port = uri.getPort() < 0 ? 443 : uri.getPort();
            if (port != 443) return Validation.denied("Fetch only permits the standard HTTPS port");
            return new Validation(true, "allowed", asciiHost, port);
        }
        catch (IllegalArgumentException | UnknownHostException ex) {
            return Validation.denied("Fetch URL cannot be safely resolved");
        }
    }

    private static boolean isSpecialHostname(String host) {
        String lower = host.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("localhost") || lower.endsWith(".localhost") || lower.endsWith(".local")
                || lower.endsWith(".internal") || lower.endsWith(".home.arpa")
                || lower.equals("metadata.google.internal") || lower.matches("[0-9]+");
    }

    static boolean isForbiddenAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return true;
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int a = bytes[0] & 0xff;
            int b = bytes[1] & 0xff;
            return a == 0 || a == 10 || a == 127 || (a == 169 && b == 254) || (a == 172 && b >= 16 && b <= 31)
                    || (a == 192 && b == 168) || (a == 100 && b >= 64 && b <= 127) || a >= 224;
        }
        if (address instanceof Inet6Address) {
            boolean mapped = bytes.length == 16
                    && java.util.stream.IntStream.range(0, 10).allMatch(i -> bytes[i] == 0)
                    && bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
            if (mapped) {
                try { return isForbiddenAddress(InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16))); }
                catch (UnknownHostException impossible) { return true; }
            }
            int first = bytes[0] & 0xff;
            return (first & 0xfe) == 0xfc || (first == 0xfe && ((bytes[1] & 0xc0) == 0x80));
        }
        return true;
    }

    public record Validation(boolean allowed, String reason, String host, int port) {
        static Validation denied(String reason) { return new Validation(false, reason, "", -1); }
    }
}
