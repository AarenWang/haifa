package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpFetchUrlPolicyTest {

    @Test
    void allowsOnlyPublicCanonicalHttps() throws Exception {
        McpFetchUrlPolicy policy = new McpFetchUrlPolicy(host -> new InetAddress[] {
                InetAddress.getByAddress(host, new byte[] { 93, (byte) 184, (byte) 216, 34 })
        });
        assertThat(policy.validate(Map.of("url", "https://example.com/article")).allowed()).isTrue();
        assertThat(policy.validate(Map.of("url", "http://example.com/")).allowed()).isFalse();
        assertThat(policy.validate(Map.of("url", "https://user:pass@example.com/")).allowed()).isFalse();
        assertThat(policy.validate(Map.of("url", "https://example.com:8443/")).allowed()).isFalse();
        assertThat(policy.validate(Map.of("url", "https://2130706433/")).allowed()).isFalse();
    }

    @Test
    void blocksPrivateMetadataAndMappedAddresses() throws Exception {
        assertDenied(new byte[] { 127, 0, 0, 1 });
        assertDenied(new byte[] { 10, 0, 0, 1 });
        assertDenied(new byte[] { (byte) 169, (byte) 254, (byte) 169, (byte) 254 });
        assertDenied(new byte[] { (byte) 192, (byte) 168, 1, 1 });
        assertDenied(InetAddress.getByName("::1").getAddress());
        assertDenied(InetAddress.getByName("::ffff:127.0.0.1").getAddress());
    }

    private static void assertDenied(byte[] address) throws Exception {
        McpFetchUrlPolicy policy = new McpFetchUrlPolicy(host -> new InetAddress[] {
                InetAddress.getByAddress(host, address)
        });
        assertThat(policy.validate(Map.of("url", "https://example.com/")).allowed()).isFalse();
    }
}
