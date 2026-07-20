package org.wrj.haifa.ai.utilitymcp.tool;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityResult;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

/**
 * Application-scoped, lazy MCP client for Microsoft Learn. The remote catalog is
 * discovered at connection time and refreshed after a transport failure; only the
 * three locally owned contracts in {@link MicrosoftLearnService} can be invoked.
 */
public class MicrosoftLearnMcpClient implements MicrosoftLearnGateway, AutoCloseable {

    private static final Set<String> SUPPORTED_TOOLS = Set.of(
            "microsoft_docs_search", "microsoft_docs_fetch", "microsoft_code_sample_search");

    private final UtilityMcpProperties.MicrosoftLearn properties;
    private final Object monitor = new Object();
    private volatile McpSyncClient client;
    private volatile Set<String> discoveredTools = Set.of();

    public MicrosoftLearnMcpClient(UtilityMcpProperties.MicrosoftLearn properties) {
        this.properties = properties;
        validateEndpoint(properties);
    }

    @Override
    public UtilityResult call(String toolName, Map<String, Object> arguments) {
        if (!properties.isEnabled()) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    "Microsoft Learn integration is disabled", false);
        }
        if (!SUPPORTED_TOOLS.contains(toolName)) {
            throw new UtilityToolException(UtilityErrorCode.POLICY_DENIED,
                    "Microsoft Learn tool is not allowlisted", false);
        }
        try {
            return invoke(ensureClient(), toolName, arguments);
        }
        catch (UtilityToolException ex) {
            throw ex;
        }
        catch (RuntimeException firstFailure) {
            // The remote server documents that schemas and tool availability can change.
            // Reinitialize once before surfacing a safe upstream failure.
            resetClient();
            try {
                return invoke(ensureClient(), toolName, arguments);
            }
            catch (UtilityToolException ex) {
                throw ex;
            }
            catch (RuntimeException retryFailure) {
                throw unavailable(retryFailure);
            }
        }
    }

    private UtilityResult invoke(McpSyncClient activeClient, String toolName, Map<String, Object> arguments) {
        if (!discoveredTools.contains(toolName)) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    "Microsoft Learn did not publish the required tool", true);
        }
        McpSchema.CallToolResult result = activeClient.callTool(new McpSchema.CallToolRequest(toolName, arguments));
        if (Boolean.TRUE.equals(result.isError())) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    "Microsoft Learn could not complete the request", true);
        }
        StringBuilder content = new StringBuilder();
        for (McpSchema.Content item : result.content() == null ? List.<McpSchema.Content>of() : result.content()) {
            if (item instanceof McpSchema.TextContent text) appendBounded(content, text.text());
        }
        if (content.isEmpty() && result.structuredContent() != null) {
            appendBounded(content, String.valueOf(result.structuredContent()));
        }
        if (content.isEmpty()) {
            throw new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                    "Microsoft Learn returned no readable content", true);
        }
        boolean partial = content.length() >= maxContentChars();
        return UtilityResult.external(Map.of("content", content.toString()), "microsoft-learn",
                properties.getEndpoint(), OffsetDateTime.now(), false, partial, null, Map.of());
    }

    private McpSyncClient ensureClient() {
        McpSyncClient existing = client;
        if (existing != null) return existing;
        synchronized (monitor) {
            if (client != null) return client;
            URI endpoint = properties.getEndpoint();
            URI origin = URI.create(endpoint.getScheme() + "://" + endpoint.getAuthority());
            String path = endpoint.getRawPath();
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(origin.toString())
                    .endpoint(StringUtils.hasText(path) ? path : "/api/mcp")
                    .connectTimeout(properties.getRequestTimeout())
                    .build();
            McpSyncClient created = McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("haifa-utility-mcp", "1.0"))
                    .requestTimeout(properties.getRequestTimeout())
                    .initializationTimeout(properties.getRequestTimeout())
                    .toolsChangeConsumer(this::updateDiscoveredTools)
                    .build();
            try {
                created.initialize();
                updateDiscoveredTools(listAllTools(created));
                client = created;
                return created;
            }
            catch (RuntimeException ex) {
                try { created.closeGracefully(); }
                catch (RuntimeException ignored) { }
                throw unavailable(ex);
            }
        }
    }

    private static List<McpSchema.Tool> listAllTools(McpSyncClient activeClient) {
        List<McpSchema.Tool> tools = new ArrayList<>();
        String cursor = null;
        Set<String> seenCursors = new LinkedHashSet<>();
        do {
            McpSchema.ListToolsResult page = cursor == null ? activeClient.listTools() : activeClient.listTools(cursor);
            if (page.tools() != null) tools.addAll(page.tools());
            cursor = page.nextCursor();
            if (cursor != null && !seenCursors.add(cursor)) {
                throw new IllegalStateException("Microsoft Learn MCP tools/list repeated a cursor");
            }
            if (tools.size() > 100) throw new IllegalStateException("Microsoft Learn MCP tool catalog exceeds the safety limit");
        }
        while (StringUtils.hasText(cursor));
        return List.copyOf(tools);
    }

    private void updateDiscoveredTools(List<McpSchema.Tool> tools) {
        discoveredTools = tools == null ? Set.of() : tools.stream()
                .map(McpSchema.Tool::name)
                .filter(SUPPORTED_TOOLS::contains)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private void resetClient() {
        synchronized (monitor) {
            McpSyncClient previous = client;
            client = null;
            discoveredTools = Set.of();
            if (previous != null) {
                try { previous.closeGracefully(); }
                catch (RuntimeException ignored) { }
            }
        }
    }

    @Override
    public void close() {
        resetClient();
    }

    private int maxContentChars() {
        return Math.max(1_000, Math.min(60_000, properties.getMaxContentChars()));
    }

    private void appendBounded(StringBuilder target, String value) {
        if (value == null || target.length() >= maxContentChars()) return;
        if (!target.isEmpty()) target.append('\n');
        String safe = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        int remaining = maxContentChars() - target.length();
        target.append(safe, 0, Math.min(safe.length(), remaining));
    }

    private static UtilityToolException unavailable(RuntimeException cause) {
        return new UtilityToolException(UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                "Microsoft Learn MCP is unavailable", true, cause);
    }

    private static void validateEndpoint(UtilityMcpProperties.MicrosoftLearn config) {
        URI endpoint = config.getEndpoint();
        if (endpoint == null || endpoint.getHost() == null || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null || endpoint.getQuery() != null) {
            throw new IllegalArgumentException("Microsoft Learn MCP endpoint must be an absolute URL without credentials, query or fragment");
        }
        boolean secure = "https".equalsIgnoreCase(endpoint.getScheme());
        boolean localHttp = config.isAllowHttpForTests() && "http".equalsIgnoreCase(endpoint.getScheme())
                && ("127.0.0.1".equals(endpoint.getHost()) || "localhost".equalsIgnoreCase(endpoint.getHost()) || "::1".equals(endpoint.getHost()));
        if (!secure && !localHttp) {
            throw new IllegalArgumentException("Microsoft Learn MCP endpoint must use HTTPS");
        }
    }
}
