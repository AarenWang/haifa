package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxCapabilityService;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.upload.UploadStorageService;
import org.wrj.haifa.ai.deerflow.mcp.McpConnectionManager;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow")
public class HealthController {

    private final DeerFlowProperties properties;
    private final ToolRegistry toolRegistry;
    private final RunManager runManager;
    private final ThreadManager threadManager;
    private final MessageStore messageStore;
    private final UploadStorageService uploadStorageService;
    private final SandboxCapabilityService sandboxCapabilityService;
    private final McpConnectionManager mcpConnectionManager;

    public HealthController(DeerFlowProperties properties, ToolRegistry toolRegistry,
                            RunManager runManager, ThreadManager threadManager, MessageStore messageStore,
                            UploadStorageService uploadStorageService,
                            SandboxCapabilityService sandboxCapabilityService,
                            McpConnectionManager mcpConnectionManager) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.runManager = runManager;
        this.threadManager = threadManager;
        this.messageStore = messageStore;
        this.uploadStorageService = uploadStorageService;
        this.sandboxCapabilityService = sandboxCapabilityService;
        this.mcpConnectionManager = mcpConnectionManager;
    }

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "haifa-ai-deerflow");
        body.put("time", Instant.now().toString());
        body.put("uploadsRoot", properties.getUploadsRoot());
        body.put("maxUploadBytes", properties.getMaxUploadBytes());
        body.put("toolCount", toolRegistry.tools().size());
        body.put("runCount", runManager.count());
        body.put("threadCount", threadManager.count());
        body.put("messageCount", messageStore.count());
        body.put("uploadCount", uploadStorageService.count());
        body.put("sandbox", sandboxCapabilityService.health());
        var mcpSnapshot = mcpConnectionManager.snapshot();
        body.put("mcp", Map.of(
                "enabled", mcpConnectionManager.isEnabled(),
                "running", mcpConnectionManager.isRunning(),
                "snapshotVersion", mcpSnapshot.version(),
                "snapshotAgeSeconds", Math.max(0, java.time.Duration.between(mcpSnapshot.generatedAt(), Instant.now()).toSeconds()),
                "connections", mcpSnapshot.connectionStates()));
        return Mono.just(body);
    }
}
