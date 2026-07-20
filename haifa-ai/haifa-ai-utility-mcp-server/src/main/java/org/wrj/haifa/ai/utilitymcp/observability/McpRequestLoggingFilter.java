package org.wrj.haifa.ai.utilitymcp.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Emits bounded, redacted lifecycle logs for the stateless Streamable HTTP MCP endpoint.
 * Request parameters, response bodies, credentials and raw peer addresses are never logged.
 */
public final class McpRequestLoggingFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(McpRequestLoggingFilter.class);
    private static final int MAX_CACHED_REQUEST_BYTES = 16_384;
    private final ObjectMapper objectMapper;

    public McpRequestLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/mcp".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long started = System.nanoTime();
        ContentCachingRequestWrapper wrapped = request instanceof ContentCachingRequestWrapper existing
                ? existing
                : new ContentCachingRequestWrapper(request, MAX_CACHED_REQUEST_BYTES);
        String peer = peerHash(request.getRemoteAddr());
        String protocolHeader = safe(request.getHeader("MCP-Protocol-Version"), 32, "none");

        log.info("event=mcp_connection_received requestId={} httpMethod={} path=/mcp peerHash={} protocolHeader={}",
                requestId, safe(request.getMethod(), 12, "UNKNOWN"), peer, protocolHeader);

        Throwable failure = null;
        try {
            filterChain.doFilter(wrapped, response);
        }
        catch (IOException | ServletException | RuntimeException ex) {
            failure = ex;
            throw ex;
        }
        finally {
            RequestMetadata metadata = metadata(wrapped.getContentAsByteArray());
            if ("initialize".equals(metadata.rpcMethod()) && response.getStatus() < 400 && failure == null) {
                log.info("event=mcp_initialize_request_completed requestId={} clientName={} clientVersion={} requestedProtocolVersion={} peerHash={}",
                        requestId, metadata.clientName(), metadata.clientVersion(), metadata.protocolVersion(), peer);
            }
            long durationMs = Math.max(0, (System.nanoTime() - started) / 1_000_000);
            log.info("event=mcp_request_completed requestId={} rpcMethod={} toolName={} status={} durationMs={} outcome={}",
                    requestId, metadata.rpcMethod(), metadata.toolName(), response.getStatus(), durationMs,
                    failure == null ? "completed" : "exception");
        }
    }

    private RequestMetadata metadata(byte[] content) {
        if (content == null || content.length == 0) return RequestMetadata.unknown();
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root == null || !root.isObject()) return RequestMetadata.unknown();
            String method = safe(root.path("method").asText("UNKNOWN"), 64, "UNKNOWN");
            JsonNode params = root.path("params");
            String toolName = "tools/call".equals(method)
                    ? safe(params.path("name").asText("none"), 128, "none")
                    : "none";
            if (!"initialize".equals(method)) {
                return new RequestMetadata(method, toolName, "none", "none", "none");
            }
            JsonNode client = params.path("clientInfo");
            return new RequestMetadata(method, toolName,
                    safe(client.path("name").asText("unknown"), 128, "unknown"),
                    safe(client.path("version").asText("unknown"), 64, "unknown"),
                    safe(params.path("protocolVersion").asText("unknown"), 32, "unknown"));
        }
        catch (IOException ex) {
            return RequestMetadata.unknown();
        }
    }

    private static String requestId(String candidate) {
        if (candidate != null && candidate.matches("[A-Za-z0-9._-]{1,64}")) return candidate;
        return UUID.randomUUID().toString();
    }

    private static String safe(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String sanitized = value.replaceAll("[\\p{Cntrl}]", "?").trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private static String peerHash(String remoteAddress) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(remoteAddress).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        }
        catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record RequestMetadata(String rpcMethod, String toolName, String clientName,
            String clientVersion, String protocolVersion) {
        private static RequestMetadata unknown() {
            return new RequestMetadata("UNKNOWN", "none", "none", "none", "none");
        }
    }
}
