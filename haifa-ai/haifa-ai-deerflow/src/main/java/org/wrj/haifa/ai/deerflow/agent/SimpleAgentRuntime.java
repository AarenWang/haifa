package org.wrj.haifa.ai.deerflow.agent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SimpleAgentRuntime implements AgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(SimpleAgentRuntime.class);

    private final DeerFlowProperties properties;
    private final ToolRegistry toolRegistry;
    private final AgentModelClient modelClient;
    private final RunManager runManager;

    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.modelClient = modelClient;
        this.runManager = runManager;
    }

    @Override
    public Flux<AgentEvent> stream(AgentRequest request) {
        return Flux.defer(() -> {
            String threadId = StringUtils.hasText(request.threadId()) ? request.threadId() : UUID.randomUUID().toString();
            String modelName = StringUtils.hasText(request.model()) ? request.model() : this.properties.getModel();
            RunRecord run = this.runManager.create(threadId, modelName, Map.of("source", "sse"));
            this.runManager.markRunning(run.runId());

            AgentRunConfig config = new AgentRunConfig(threadId, run.runId(), modelName, true, false,
                    this.properties.getMaxIterations(), Path.of(this.properties.getWorkspaceRoot()),
                    Map.of("runtime", "simple-agent-runtime"));
            AtomicInteger seq = new AtomicInteger();

            List<AgentEvent> prefixEvents = new ArrayList<>();
            prefixEvents.add(event(seq, config, AgentEventType.RUN_STARTED, "Run started", Map.of()));

            List<ToolResult> toolResults = executePlannedTools(request, config, seq, prefixEvents);
            prefixEvents.add(event(seq, config, AgentEventType.MODEL_STARTED, "Calling Spring AI model adapter",
                    Map.of("model", nullToEmpty(modelName))));

            ModelPrompt prompt = new ModelPrompt(this.properties.getSystemPrompt(),
                    buildUserPrompt(request.message(), toolResults), modelName);

            Mono<List<AgentEvent>> modelAndTerminalEvents = this.modelClient.generate(prompt)
                    .map(answer -> {
                        this.runManager.markCompleted(run.runId());
                        AgentEvent modelCompleted = event(seq, config, AgentEventType.MODEL_COMPLETED, answer,
                                Map.of("toolCount", toolResults.size()));
                        AgentEvent runCompleted = event(seq, config, AgentEventType.RUN_COMPLETED, "Run completed",
                                Map.of("status", "COMPLETED"));
                        return List.of(modelCompleted, runCompleted);
                    })
                    .onErrorResume(ex -> {
                        this.runManager.markFailed(run.runId(), ex.getMessage());
                        return Mono.just(List.of(event(seq, config, AgentEventType.RUN_FAILED, ex.getMessage(),
                                Map.of("status", "FAILED"))));
                    });

            return Flux.concat(Flux.fromIterable(prefixEvents), modelAndTerminalEvents.flatMapMany(Flux::fromIterable))
                    .doOnCancel(() -> this.runManager.markCancelled(run.runId()));
        });
    }

    private List<ToolResult> executePlannedTools(AgentRequest request, AgentRunConfig config, AtomicInteger seq,
            List<AgentEvent> events) {
        ToolRequest toolRequest = new ToolRequest(request.message(), config.workspaceRoot());
        List<ToolResult> results = new ArrayList<>();
        for (AgentTool tool : this.toolRegistry.plan(request.message(), config.maxIterations())) {
            events.add(event(seq, config, AgentEventType.TOOL_STARTED, "Executing " + tool.name(),
                    Map.of("tool", tool.name(), "description", tool.description())));
            ToolResult result = executeToolSafely(tool, toolRequest);
            results.add(result);
            events.add(event(seq, config, AgentEventType.TOOL_COMPLETED, result.content(),
                    Map.of("tool", result.toolName())));
        }
        return results;
    }

    private static ToolResult executeToolSafely(AgentTool tool, ToolRequest toolRequest) {
        try {
            return tool.execute(toolRequest);
        }
        catch (RuntimeException ex) {
            log.warn("Tool {} failed: {}", tool.name(), ex.getMessage());
            return ToolResult.of(tool.name(), "Tool failed: " + ex.getMessage());
        }
    }

    private static String buildUserPrompt(String userMessage, List<ToolResult> toolResults) {
        StringBuilder builder = new StringBuilder();
        builder.append("User request:\n").append(userMessage == null ? "" : userMessage).append("\n\n");
        if (toolResults.isEmpty()) {
            builder.append("No local tools were triggered for this request.\n");
            return builder.toString();
        }
        builder.append("Local tool observations:\n");
        for (ToolResult result : toolResults) {
            builder.append("\n<tool name=\"").append(result.toolName()).append("\">\n")
                    .append(result.content())
                    .append("\n</tool>\n");
        }
        return builder.toString();
    }

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata) {
        return AgentEvent.of(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type, content,
                metadata);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
