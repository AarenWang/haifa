package org.wrj.haifa.ai.utilitymcp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

class OriginRateLimitFilterTest {

    @Test
    void rejectsMissingAndUnknownOriginAndLimitsAcceptedSubject() throws Exception {
        UtilityMcpProperties properties = new UtilityMcpProperties();
        properties.getSecurity().setAllowMissingOrigin(false);
        properties.getSecurity().setAllowedOrigins(List.of("https://agent.example"));
        properties.getSecurity().setRequestsPerMinute(1);
        OriginRateLimitFilter filter = new OriginRateLimitFilter(properties,
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        assertThat(invoke(filter, null).getStatus()).isEqualTo(403);
        assertThat(invoke(filter, "https://evil.example").getStatus()).isEqualTo(403);
        assertThat(invoke(filter, "https://agent.example").getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = invoke(filter, "https://agent.example");
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
    }

    private static MockHttpServletResponse invoke(OriginRateLimitFilter filter, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setRemoteAddr("192.0.2.10");
        if (origin != null) request.addHeader("Origin", origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
