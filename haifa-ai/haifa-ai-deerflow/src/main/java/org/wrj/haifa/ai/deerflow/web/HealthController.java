package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.upload.UploadStorageService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow")
public class HealthController {

    private final DeerFlowProperties properties;
    private final ToolRegistry toolRegistry;
    private final RunManager runManager;
    private final UploadStorageService uploadStorageService;

    public HealthController(DeerFlowProperties properties, ToolRegistry toolRegistry,
                            RunManager runManager, UploadStorageService uploadStorageService) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.runManager = runManager;
        this.uploadStorageService = uploadStorageService;
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
        body.put("uploadCount", uploadStorageService.count());
        return Mono.just(body);
    }
}
