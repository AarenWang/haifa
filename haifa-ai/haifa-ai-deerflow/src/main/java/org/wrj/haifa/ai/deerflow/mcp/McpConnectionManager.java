package org.wrj.haifa.ai.deerflow.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.File;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.ToolDescriptor;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

@Component
public class McpConnectionManager implements McpClientAdapter, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(McpConnectionManager.class);
    private static final Set<String> ALLOWED_STDIO_COMMANDS = Set.of("uvx", "npx", "node", "python", "python3", "java");
    private final DeerFlowProperties properties;
    private final ObjectMapper objectMapper;
    private final McpResultMapper resultMapper;
    private final McpFetchUrlPolicy fetchUrlPolicy;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, ConnectionRuntime> runtimes = new ConcurrentHashMap<>();
    private final AtomicReference<ToolCatalogSnapshot> snapshot = new AtomicReference<>(ToolCatalogSnapshot.empty());
    private final AtomicLong versions = new AtomicLong();
    private volatile boolean running;

    public McpConnectionManager(DeerFlowProperties properties, ObjectMapper objectMapper, McpResultMapper resultMapper) {
        this(properties, objectMapper, resultMapper, new McpFetchUrlPolicy());
    }

    public McpConnectionManager(DeerFlowProperties properties, ObjectMapper objectMapper, McpResultMapper resultMapper,
            McpFetchUrlPolicy fetchUrlPolicy) {
        this(properties, objectMapper, resultMapper, fetchUrlPolicy, (MeterRegistry) null);
    }

    @Autowired
    public McpConnectionManager(DeerFlowProperties properties, ObjectMapper objectMapper, McpResultMapper resultMapper,
            McpFetchUrlPolicy fetchUrlPolicy, ObjectProvider<MeterRegistry> meterRegistry) {
        this(properties, objectMapper, resultMapper, fetchUrlPolicy, meterRegistry.getIfAvailable());
    }

    private McpConnectionManager(DeerFlowProperties properties, ObjectMapper objectMapper, McpResultMapper resultMapper,
            McpFetchUrlPolicy fetchUrlPolicy, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resultMapper = resultMapper;
        this.fetchUrlPolicy = fetchUrlPolicy;
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            io.micrometer.core.instrument.Gauge.builder("mcp.registry.snapshot.version", versions, AtomicLong::get)
                    .register(meterRegistry);
        }
    }

    @Override
    public synchronized void start() {
        if (running || !isEnabled()) return;
        validateConnectionNames();
        List<String> requiredFailures = new ArrayList<>();
        properties.getMcp().getServers().forEach((name, config) -> {
            if (!config.isEnabled()) return;
            try {
                startConnection(name, config);
            }
            catch (RuntimeException ex) {
                McpConnectionState state = isAuthenticationFailure(ex) ? McpConnectionState.AUTH_FAILED : McpConnectionState.DEGRADED;
                runtimes.put(normalize(name), ConnectionRuntime.failed(normalize(name), config, state, ex));
                log.warn("MCP connection {} failed to start: {}", normalize(name), ex.getClass().getSimpleName());
                if (config.isRequired()) requiredFailures.add(normalize(name));
            }
        });
        rebuildSnapshot();
        if (!requiredFailures.isEmpty()) {
            stop();
            throw new IllegalStateException("Required MCP connections failed: " + requiredFailures);
        }
        running = true;
    }

    private void startConnection(String configuredName, DeerFlowProperties.McpServer config) {
        long started = System.nanoTime();
        String name = normalize(configuredName);
        McpClientTransport transport = createTransport(config);
        Duration timeout = Duration.ofMillis(config.getRequestTimeoutMs());
        McpSyncClient client = null;
        try {
            client = McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("haifa-deerflow", "1.0"))
                    .requestTimeout(timeout)
                    .initializationTimeout(timeout)
                    .toolsChangeConsumer(tools -> publishDiscoveredTools(name, tools))
                    .build();
            ConnectionRuntime runtime = new ConnectionRuntime(name, config, client, McpConnectionState.STARTING,
                    Map.of(), Instant.EPOCH, null);
            runtimes.put(name, runtime);
            client.initialize();
            publishDiscoveredTools(name, listAllTools(client));
            recordDuration("mcp.connection.initialize.duration", "success", started);
        }
        catch (RuntimeException ex) {
            recordDuration("mcp.connection.initialize.duration", "failed", started);
            if (client != null) {
                try { client.closeGracefully(); }
                catch (RuntimeException closeFailure) {
                    log.debug("MCP client close after failed initialization failed for {}", name);
                }
            }
            throw ex;
        }
    }

    private McpClientTransport createTransport(DeerFlowProperties.McpServer config) {
        String transport = config.getTransport() == null ? "" : config.getTransport().trim().toUpperCase(Locale.ROOT);
        return switch (transport) {
            case "STREAMABLE_HTTP", "STREAMABLE-HTTP", "HTTP" -> createHttpTransport(config);
            case "STDIO" -> createStdioTransport(config);
            default -> throw new IllegalArgumentException("Unsupported MCP transport: " + config.getTransport());
        };
    }

    private McpClientTransport createHttpTransport(DeerFlowProperties.McpServer config) {
        URI base = URI.create(config.getUrl());
        boolean localHttp = "http".equalsIgnoreCase(base.getScheme())
                && ("127.0.0.1".equals(base.getHost()) || "localhost".equalsIgnoreCase(base.getHost()) || "::1".equals(base.getHost()));
        if ((!"https".equalsIgnoreCase(base.getScheme()) && !localHttp) || base.getUserInfo() != null || base.getHost() == null) {
            throw new IllegalArgumentException("Remote MCP URL must use HTTPS; loopback HTTP is allowed for local development");
        }
        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(base.toString())
                .endpoint(StringUtils.hasText(config.getEndpoint()) ? config.getEndpoint() : "/mcp")
                .connectTimeout(Duration.ofMillis(Math.min(config.getRequestTimeoutMs(), 10_000)));
        builder.customizeRequest(request -> addHttpHeaders(request, config));
        return builder.build();
    }

    private static void addHttpHeaders(HttpRequest.Builder request, DeerFlowProperties.McpServer config) {
        if (StringUtils.hasText(config.getOrigin())) request.header("Origin", config.getOrigin());
        if (StringUtils.hasText(config.getBearerTokenEnvironment())) {
            String token = System.getenv(config.getBearerTokenEnvironment());
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("Configured MCP bearer-token environment variable is missing");
            }
            request.header("Authorization", "Bearer " + token);
        }
    }

    private McpClientTransport createStdioTransport(DeerFlowProperties.McpServer config) {
        String command = config.getCommand();
        if (!StringUtils.hasText(command)) throw new IllegalArgumentException("STDIO MCP command is required");
        String basename = Path.of(command).getFileName().toString().toLowerCase(Locale.ROOT).replaceFirst("\\.exe$", "");
        if (!ALLOWED_STDIO_COMMANDS.contains(basename)) {
            throw new IllegalArgumentException("STDIO MCP command is not allowlisted: " + basename);
        }
        if (config.getArgs().stream().anyMatch(McpConnectionManager::containsShellControl)) {
            throw new IllegalArgumentException("STDIO MCP arguments must not contain shell control operators");
        }
        Map<String, String> environment = new LinkedHashMap<>();
        for (String name : config.getEnvironmentNames()) {
            if (isSensitiveEnvironmentName(name)) {
                throw new IllegalArgumentException("Sensitive environment variable is not allowed for STDIO MCP: " + name);
            }
            String value = System.getenv(name);
            if (value != null) environment.put(name, value);
        }
        ServerParameters parameters = ServerParameters.builder(command).args(config.getArgs()).env(environment).build();
        File cwd = null;
        if (StringUtils.hasText(config.getCwd())) {
            Path path = Path.of(config.getCwd()).toAbsolutePath().normalize();
            if (!path.toFile().isDirectory()) throw new IllegalArgumentException("STDIO MCP cwd is not a directory");
            cwd = path.toFile();
        }
        return new RestrictedStdioClientTransport(parameters,
                new JacksonMcpJsonMapper(objectMapper.copy()), cwd);
    }

    private List<McpSchema.Tool> listAllTools(McpSyncClient client) {
        List<McpSchema.Tool> tools = new ArrayList<>();
        String cursor = null;
        Set<String> seenCursors = new HashSet<>();
        do {
            McpSchema.ListToolsResult page = cursor == null ? client.listTools() : client.listTools(cursor);
            if (page.tools() != null) tools.addAll(page.tools());
            cursor = page.nextCursor();
            if (cursor != null && !seenCursors.add(cursor)) {
                throw new IllegalStateException("MCP tools/list pagination repeated a cursor");
            }
            if (tools.size() > 10_000) throw new IllegalStateException("MCP tool catalog exceeds the safety limit");
        }
        while (StringUtils.hasText(cursor));
        return List.copyOf(tools);
    }

    private synchronized void publishDiscoveredTools(String connectionName, List<McpSchema.Tool> discovered) {
        long started = System.nanoTime();
        ConnectionRuntime runtime = runtimes.get(connectionName);
        if (runtime == null) return;
        long version = versions.incrementAndGet();
        Map<String, McpToolIdentity> mapped = new LinkedHashMap<>();
        for (McpSchema.Tool tool : discovered == null ? List.<McpSchema.Tool>of() : discovered) {
            if (!allowed(runtime.config, tool.name())) continue;
            McpRiskClassification localRisk = parseEnum(McpRiskClassification.class,
                    runtime.config.getRiskMappings().getOrDefault(tool.name(), runtime.config.getDefaultRisk()),
                    McpRiskClassification.UNKNOWN);
            if (localRisk == McpRiskClassification.UNKNOWN) continue;
            String exposed = exposedName(connectionName, tool.name());
            if (mapped.containsKey(exposed)) throw new IllegalStateException("MCP tool name collision: " + exposed);
            mapped.put(exposed, new McpToolIdentity(
                    exposed,
                    connectionName,
                    tool.name(),
                    version,
                    sanitizeDescription(tool.description()),
                    schemaJson(tool.inputSchema()),
                    tool.annotations(),
                    localRisk,
                    parseEnum(McpSemanticType.class, runtime.config.getSemanticMappings().get(tool.name()), McpSemanticType.GENERIC),
                    parseNullableEnum(McpCapabilityOwner.class, runtime.config.getCapabilityMappings().get(tool.name()))));
        }
        runtimes.put(connectionName, runtime.ready(mapped, Instant.now()));
        rebuildSnapshot();
        if (meterRegistry != null) {
            meterRegistry.summary("mcp.discovery.tool.count").record(mapped.size());
        }
        recordDuration("mcp.discovery.duration", "success", started);
    }

    private synchronized void rebuildSnapshot() {
        Map<String, McpToolIdentity> tools = new LinkedHashMap<>();
        Map<String, McpConnectionStatus> statuses = new LinkedHashMap<>();
        Map<McpCapabilityOwner, String> observedOwners = new HashMap<>();
        Map<String, String> configuredOwners = properties.getMcp().getCapabilityOwners();
        for (ConnectionRuntime runtime : runtimes.values().stream().sorted(java.util.Comparator.comparing(item -> item.name)).toList()) {
            statuses.put(runtime.name, runtime.status());
            for (McpToolIdentity identity : runtime.tools.values()) {
                if (identity.capabilityOwner() != null) {
                    String configured = configuredOwners.get(identity.capabilityOwner().name());
                    if (configured != null && !normalize(configured).equals(identity.connectionName())) continue;
                    String prior = observedOwners.putIfAbsent(identity.capabilityOwner(), identity.connectionName());
                    if (configured == null && prior != null && !prior.equals(identity.connectionName())) {
                        throw new IllegalStateException("MCP capability owner conflict for " + identity.capabilityOwner());
                    }
                }
                if (tools.putIfAbsent(identity.exposedName(), identity) != null) {
                    throw new IllegalStateException("MCP exposed tool collision: " + identity.exposedName());
                }
            }
        }
        long version = versions.incrementAndGet();
        snapshot.set(new ToolCatalogSnapshot(version, Instant.now(), tools, statuses));
    }

    public ToolResult execute(McpToolIdentity requestedIdentity, String argumentsJson) {
        return execute(requestedIdentity, argumentsJson, null, null);
    }

    public ToolResult execute(McpToolIdentity requestedIdentity, String argumentsJson, String threadId, String runId) {
        ToolCatalogSnapshot current = snapshot.get();
        McpToolIdentity identity = current.toolsByExposedName().get(requestedIdentity.exposedName());
        if (identity == null || !identity.connectionName().equals(requestedIdentity.connectionName())
                || !identity.originalToolName().equals(requestedIdentity.originalToolName())) {
            return ToolResult.denied(requestedIdentity.exposedName(), "MCP tool is no longer available in the active catalog",
                    Map.of("source", "mcp", "reason", "STALE_IDENTITY"));
        }
        ConnectionRuntime runtime = runtimes.get(identity.connectionName());
        if (runtime == null || runtime.client == null) return unavailable(identity, "MCP connection is not available");
        if (runtime.state == McpConnectionState.STALE
                && parseEnum(McpStalePolicy.class, runtime.config.getStalePolicy(), McpStalePolicy.DENY_NEW_CALLS) == McpStalePolicy.DENY_NEW_CALLS) {
            return unavailable(identity, "MCP catalog is stale and new calls are denied");
        }
        long started = System.nanoTime();
        try {
            Map<String, Object> arguments = parseArguments(argumentsJson);
            if (identity.semanticType() == McpSemanticType.WEB_FETCH) {
                McpFetchUrlPolicy.Validation validation = fetchUrlPolicy.validate(arguments);
                if (!validation.allowed()) {
                    audit(McpAuditEvent.Type.POLICY_DENIED, identity);
                    return ToolResult.denied(identity.exposedName(), validation.reason(),
                            Map.of("source", "mcp", "connectionName", identity.connectionName(),
                                    "errorType", "POLICY_DENIED"));
                }
            }
            McpSchema.CallToolResult result = runtime.client.callTool(new McpSchema.CallToolRequest(identity.originalToolName(), arguments));
            ToolResult mapped = resultMapper.map(identity, result, threadId, runId);
            recordDuration("mcp.tool.call.duration", mapped.status().name().toLowerCase(Locale.ROOT), started);
            if (meterRegistry != null) meterRegistry.summary("mcp.result.bytes").record(mapped.content().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            audit(mapped.status() == ToolResult.Status.SUCCESS ? McpAuditEvent.Type.TOOL_SUCCEEDED : McpAuditEvent.Type.TOOL_FAILED, identity);
            return mapped;
        }
        catch (RuntimeException ex) {
            recordDuration("mcp.tool.call.duration", classifyFailure(ex).toLowerCase(Locale.ROOT), started);
            audit(switch (classifyFailure(ex)) {
                case "AUTH_FAILED" -> McpAuditEvent.Type.AUTH_FAILED;
                case "PROTOCOL_FAILED" -> McpAuditEvent.Type.PROTOCOL_FAILED;
                default -> McpAuditEvent.Type.TRANSPORT_FAILED;
            }, identity);
            return ToolResult.failed(identity.exposedName(), "MCP tool call failed",
                    Map.of("source", "mcp", "connectionName", identity.connectionName(),
                            "errorType", classifyFailure(ex)));
        }
    }

    @Override
    public ToolResult executeToolResult(String toolName, String argumentsJson) {
        McpToolIdentity identity = snapshot.get().toolsByExposedName().get(toolName);
        return identity == null ? ToolResult.notFound(toolName, "MCP tool not found") : execute(identity, argumentsJson);
    }

    @Override
    public String executeTool(String toolName, String argumentsJson) {
        return executeToolResult(toolName, argumentsJson).content();
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return snapshot.get().toolsByExposedName().values().stream()
                .map(tool -> new ToolDescriptor(tool.exposedName(), tool.description(), "mcp", "MCP 动态工具", false,
                        "connection=" + tool.connectionName() + ", risk=" + tool.localRiskClassification()
                                + ", semantic=" + tool.semanticType()))
                .toList();
    }

    public ToolCatalogSnapshot snapshot() { return snapshot.get(); }
    public java.util.Optional<McpToolIdentity> findIdentity(String exposedName) {
        return java.util.Optional.ofNullable(snapshot.get().toolsByExposedName().get(exposedName));
    }

    public synchronized boolean refresh(String connectionName) {
        String normalized = normalize(connectionName);
        ConnectionRuntime runtime = runtimes.get(normalized);
        if (runtime == null || runtime.client == null) return false;
        try {
            publishDiscoveredTools(normalized, listAllTools(runtime.client));
            increment("mcp.discovery.refresh", "success");
            return true;
        }
        catch (RuntimeException ex) {
            runtimes.put(normalized, runtime.stale(ex));
            rebuildSnapshot();
            increment("mcp.discovery.refresh", "stale");
            return false;
        }
    }
    @Override public boolean isEnabled() { return properties.isMcpEnabled(); }
    @Override public boolean isRunning() { return running; }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE - 100; }

    @Override
    public synchronized void stop() {
        runtimes.values().forEach(runtime -> {
            if (runtime.client != null) {
                try { runtime.client.closeGracefully(); }
                catch (RuntimeException ex) { log.debug("MCP client close failed for {}", runtime.name); }
            }
        });
        runtimes.clear();
        snapshot.set(ToolCatalogSnapshot.empty());
        running = false;
    }

    private void validateConnectionNames() {
        Set<String> normalized = new HashSet<>();
        properties.getMcp().getServers().forEach((name, config) -> {
            if (config.isEnabled() && !normalized.add(normalize(name))) {
                throw new IllegalArgumentException("MCP connection name collision after normalization: " + name);
            }
        });
    }

    private Map<String, Object> parseArguments(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try {
            Map<String, Object> value = objectMapper.readValue(json, new TypeReference<>() {});
            return value == null ? Map.of() : value;
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("MCP tool arguments must be a JSON object", ex);
        }
    }

    private String schemaJson(McpSchema.JsonSchema schema) {
        if (schema == null || !"object".equals(schema.type())) {
            throw new IllegalArgumentException("MCP input schema must be an object schema");
        }
        try {
            String json = objectMapper.writeValueAsString(schema);
            if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65_536) {
                throw new IllegalArgumentException("MCP input schema exceeds the 64 KiB safety limit");
            }
            return json;
        }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("MCP input schema is not serializable", ex); }
    }

    private static boolean allowed(DeerFlowProperties.McpServer config, String tool) {
        return !config.getAllowedTools().isEmpty()
                && config.getAllowedTools().contains(tool)
                && !config.getDeniedTools().contains(tool);
    }

    private static String exposedName(String connection, String tool) {
        return "mcp__" + connection + "__" + normalize(tool);
    }

    static String normalize(String value) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException("MCP name must not be blank");
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[-_]+|[-_]+$", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("MCP name has no safe characters");
        return normalized;
    }

    private static String sanitizeDescription(String description) {
        String value = description == null ? "MCP tool" : description.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        return value.length() <= 2_000 ? value : value.substring(0, 2_000);
    }

    private static boolean containsShellControl(String value) {
        return value != null && (value.contains("&&") || value.contains("||") || value.contains(";")
                || value.contains("|") || value.contains("`") || value.contains("$("));
    }

    private static boolean isSensitiveEnvironmentName(String name) {
        if (!StringUtils.hasText(name)) return true;
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.matches(".*(API_KEY|TOKEN|SECRET|PASSWORD|PASSWD|PRIVATE_KEY|CREDENTIALS?|ACCESS_KEY|SESSION_KEY).*");
    }

    private static boolean isAuthenticationFailure(RuntimeException ex) {
        String value = String.valueOf(ex.getMessage()).toLowerCase(Locale.ROOT);
        return value.contains("401") || value.contains("403") || value.contains("unauthorized") || value.contains("forbidden");
    }

    private static String classifyFailure(RuntimeException ex) {
        if (isAuthenticationFailure(ex)) return "AUTH_FAILED";
        String value = (ex.getClass().getName() + " " + ex.getMessage()).toLowerCase(Locale.ROOT);
        if (value.contains("protocol") || value.contains("json-rpc") || value.contains("jsonrpc")
                || value.contains("parse")) return "PROTOCOL_FAILED";
        return "TRANSPORT_FAILED";
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E defaultValue) {
        if (!StringUtils.hasText(value)) return defaultValue;
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": " + value); }
    }

    private static <E extends Enum<E>> E parseNullableEnum(Class<E> type, String value) {
        return StringUtils.hasText(value) ? parseEnum(type, value, null) : null;
    }

    private static ToolResult unavailable(McpToolIdentity identity, String message) {
        return ToolResult.failed(identity.exposedName(), message,
                Map.of("source", "mcp", "connectionName", identity.connectionName(), "errorType", "CONNECTION_UNAVAILABLE"));
    }

    private void recordDuration(String metric, String result, long startedNanos) {
        if (meterRegistry == null) return;
        Timer.builder(metric).tag("result", result).register(meterRegistry)
                .record(Duration.ofNanos(System.nanoTime() - startedNanos));
    }

    private void increment(String metric, String result) {
        if (meterRegistry != null) meterRegistry.counter(metric, "result", result).increment();
    }

    private static void audit(McpAuditEvent.Type type, McpToolIdentity identity) {
        log.info("MCP audit type={} connection={} tool={} snapshot={}", type, identity.connectionName(),
                identity.originalToolName(), identity.discoverySnapshotVersion());
    }

    private static final class ConnectionRuntime {
        private final String name;
        private final DeerFlowProperties.McpServer config;
        private final McpSyncClient client;
        private final McpConnectionState state;
        private final Map<String, McpToolIdentity> tools;
        private final Instant lastSuccessfulDiscovery;
        private final String errorType;

        private ConnectionRuntime(String name, DeerFlowProperties.McpServer config, McpSyncClient client,
                McpConnectionState state, Map<String, McpToolIdentity> tools, Instant lastSuccessfulDiscovery,
                String errorType) {
            this.name = name;
            this.config = config;
            this.client = client;
            this.state = state;
            this.tools = Map.copyOf(tools);
            this.lastSuccessfulDiscovery = lastSuccessfulDiscovery;
            this.errorType = errorType;
        }

        private static ConnectionRuntime failed(String name, DeerFlowProperties.McpServer config,
                McpConnectionState state, RuntimeException ex) {
            return new ConnectionRuntime(name, config, null, state, Map.of(), Instant.EPOCH, ex.getClass().getSimpleName());
        }

        private ConnectionRuntime ready(Map<String, McpToolIdentity> tools, Instant at) {
            return new ConnectionRuntime(name, config, client, McpConnectionState.READY, tools, at, null);
        }

        private ConnectionRuntime stale(RuntimeException ex) {
            return new ConnectionRuntime(name, config, client, McpConnectionState.STALE, tools,
                    lastSuccessfulDiscovery, ex.getClass().getSimpleName());
        }

        private McpConnectionStatus status() {
            return new McpConnectionStatus(name, state, config.isRequired(), tools.size(),
                    tools.values().stream().mapToLong(McpToolIdentity::discoverySnapshotVersion).max().orElse(0),
                    lastSuccessfulDiscovery, errorType);
        }
    }
}
