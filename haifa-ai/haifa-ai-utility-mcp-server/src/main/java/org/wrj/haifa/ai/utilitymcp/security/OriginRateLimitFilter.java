package org.wrj.haifa.ai.utilitymcp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

public class OriginRateLimitFilter extends OncePerRequestFilter {

    private final UtilityMcpProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public OriginRateLimitFilter(UtilityMcpProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!originAllowed(request.getHeader("Origin"))) {
            reject(response, 403, "POLICY_DENIED", "Origin is not allowed");
            return;
        }
        if (!consume(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            reject(response, 429, "POLICY_DENIED", "MCP request rate limit exceeded");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean originAllowed(String origin) {
        UtilityMcpProperties.Security security = properties.getSecurity();
        if (origin == null || origin.isBlank()) return security.isAllowMissingOrigin();
        Set<String> allowed = Set.copyOf(security.getAllowedOrigins());
        return allowed.contains(origin);
    }

    private boolean consume(String subject) {
        int limit = Math.max(1, properties.getSecurity().getRequestsPerMinute());
        long minute = Instant.now(clock).getEpochSecond() / 60;
        String key = (subject == null ? "unknown" : subject) + ':' + minute;
        Window window = windows.computeIfAbsent(key, ignored -> new Window(minute));
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> entry.getValue().minute < minute - 1);
        return window.count.incrementAndGet() <= limit;
    }

    private static void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();
        private Window(long minute) { this.minute = minute; }
    }
}
