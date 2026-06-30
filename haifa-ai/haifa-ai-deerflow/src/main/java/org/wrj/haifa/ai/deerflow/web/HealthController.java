package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/deerflow")
public class HealthController {

    private final DeerFlowProperties properties;
    private final ToolRegistry toolRegistry;

    public HealthController(DeerFlowProperties properties, ToolRegistry toolRegistry) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
    }

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "haifa-ai-deerflow");
        body.put("time", Instant.now().toString());
        body.put("uploadsRoot", properties.getUploadsRoot());
        body.put("toolCount", toolRegistry.tools().size());
        return Mono.just(body);
    }
}
