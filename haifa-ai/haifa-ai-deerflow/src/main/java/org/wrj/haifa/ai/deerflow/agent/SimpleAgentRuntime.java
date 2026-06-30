package org.wrj.haifa.ai.deerflow.agent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
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
    private final ThreadManager threadManager;
    private final MessageStore messageStore;
    private final List<AgentMiddleware> middlewares;

    @Autowired(required = false)
    private ToolPolicyService toolPolicyService;

    @Autowired(required = false)
    private SlashSkillResolver slashSkillResolver;

    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.modelClient = modelClient;
        this.runManager = runManager;
        this.threadManager = threadManager;
        this.messageStore = messageStore;
        this.middlewares = middlewares.stream()
                .sorted(Comparator.comparingInt(m -> {
                    MiddlewareOrder order = m.getClass().getAnnotation(MiddlewareOrder.class);
                    return order == null ? Integer.MAX_VALUE : order.value();
                }))
                .toList();
    }

    @Override
    public Flux<AgentEvent> stream(AgentRequest request) {
        return Flux.defer(() -> {
            long runStartTime = System.currentTimeMillis();
            String requestedThreadId = StringUtils.hasText(request.threadId()) ? request.threadId() : UUID.randomUUID().toString();
            ThreadRecord thread = this.threadManager.upsert(requestedThreadId,
                    ThreadManager.titleFromMessage(request.message()), Map.of("source", "run"));
            String threadId = thread.threadId();
            String modelName = StringUtils.hasText(request.model()) ? request.model() : this.properties.getModel();
            int uploadedFileCount = request.uploadedFileIds() == null ? 0 : request.uploadedFileIds().size();
            RunRecord run = this.runManager.create(threadId, modelName, Map.of("source", "sse", "uploadedFiles", uploadedFileCount));
            this.runManager.markRunning(run.runId());
            this.messageStore.add(threadId, run.runId(), MessageRole.USER, request.message(),
                    Map.of("uploadedFileCount", uploadedFileCount));
            log.info("Run started. runId={}, threadId={}, model={}, uploadedFileCount={}", run.runId(), threadId, nullToEmpty(modelName), uploadedFileCount);

            AgentRunConfig config = new AgentRunConfig(threadId, run.runId(), modelName, true, false,
                    this.properties.getMaxIterations(), Path.of(this.properties.getWorkspaceRoot()),
                    Map.of("runtime", "simple-agent-runtime"));
            AtomicInteger seq = new AtomicInteger();

            List<AgentEvent> prefixEvents = new ArrayList<>();
            prefixEvents.add(event(seq, config, AgentEventType.RUN_STARTED, "Run started", Map.of("uploadedFileCount", uploadedFileCount)));

            List<Skill> activeSkills = resolveActiveSkills(request);
            List<ToolResult> toolResults = executePlannedTools(request, config, seq, prefixEvents, activeSkills);
            for (ToolResult toolResult : toolResults) {
                this.messageStore.add(threadId, run.runId(), MessageRole.TOOL, toolResult.content(),
                        Map.of("tool", toolResult.toolName()));
            }

            AgentRuntimeContext runtimeContext = AgentRuntimeContext.of(config, request, toolResults, this.properties, activeSkills);
            Mono<ModelPrompt> promptMono = new MiddlewareChain(this.middlewares).next(runtimeContext);

            long modelStartTime = System.currentTimeMillis();
            Flux<AgentEvent> modelAndTerminalEvents = promptMono.flatMapMany(prompt -> {
                if (prompt.userPrompt() != null && prompt.userPrompt().startsWith("BUDGET_EXCEEDED:")) {
                    this.runManager.markCompleted(run.runId());
                    this.threadManager.touch(threadId);
                    this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT,
                            "Budget exceeded: request + observations too large.",
                            Map.of("budgetExceeded", true, "toolCount", toolResults.size()));
                    long totalDuration = System.currentTimeMillis() - runStartTime;
                    log.info("Run completed with budget limit. runId={}, totalDurationMs={}, toolCount={}", run.runId(), totalDuration, toolResults.size());
                    return Flux.fromIterable(List.of(
                            event(seq, config, AgentEventType.MODEL_COMPLETED,
                                    "Budget exceeded: request + observations too large.",
                                    Map.of("budgetExceeded", true, "toolCount", toolResults.size())),
                            event(seq, config, AgentEventType.RUN_COMPLETED, "Run completed with budget limit",
                                    Map.of("status", "BUDGET_LIMITED", "totalDurationMs", totalDuration))
                    ));
                }
                return Flux.concat(
                        Flux.just(event(seq, config, AgentEventType.MODEL_STARTED, "Calling Spring AI model adapter",
                                Map.of("model", nullToEmpty(modelName)))),
                        this.modelClient.generate(prompt)
                                .flatMapMany(answer -> {
                                    this.runManager.markCompleted(run.runId());
                                    this.threadManager.touch(threadId);
                                    this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT, answer,
                                            Map.of("toolCount", toolResults.size()));
                                    long modelDuration = System.currentTimeMillis() - modelStartTime;
                                    long totalDuration = System.currentTimeMillis() - runStartTime;
                                    log.info("Run completed. runId={}, modelDurationMs={}, totalDurationMs={}, toolCount={}, answerLength={}",
                                            run.runId(), modelDuration, totalDuration, toolResults.size(), answer.length());
                                    return Flux.fromIterable(List.of(
                                            event(seq, config, AgentEventType.MODEL_COMPLETED, answer,
                                                    Map.of("toolCount", toolResults.size(), "modelDurationMs", modelDuration)),
                                            event(seq, config, AgentEventType.RUN_COMPLETED, "Run completed",
                                                    Map.of("status", "COMPLETED", "totalDurationMs", totalDuration))
                                    ));
                                })
                                .onErrorResume(ex -> {
                                    String errorMessage = describeException(ex);
                                    long totalDuration = System.currentTimeMillis() - runStartTime;
                                    log.error("DeerFlow run failed during model generation. runId={}, threadId={}, model={}, toolCount={}, totalDurationMs={}",
                                            run.runId(), threadId, nullToEmpty(modelName), toolResults.size(), totalDuration, ex);
                                    this.runManager.markFailed(run.runId(), errorMessage);
                                    this.threadManager.touch(threadId);
                                    this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, errorMessage,
                                            Map.of("status", "FAILED", "errorType", ex.getClass().getName()));
                                    return Flux.fromIterable(List.of(
                                            event(seq, config, AgentEventType.RUN_FAILED, errorMessage,
                                                    Map.of("status", "FAILED", "errorType", ex.getClass().getName(), "totalDurationMs", totalDuration))
                                    ));
                                })
                );
            });

            return Flux.concat(Flux.fromIterable(prefixEvents), modelAndTerminalEvents)
                    .doOnCancel(() -> {
                        long totalDuration = System.currentTimeMillis() - runStartTime;
                        log.info("Run cancelled. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        this.runManager.markCancelled(run.runId());
                        this.threadManager.touch(threadId);
                        this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, "Run cancelled",
                                Map.of("status", "CANCELLED", "totalDurationMs", totalDuration));
                    });
        });
    }

    private List<Skill> resolveActiveSkills(AgentRequest request) {
        if (this.slashSkillResolver == null || !this.properties.isSkillsEnabled()) {
            return List.of();
        }
        return this.slashSkillResolver.resolve(request.message());
    }

    private List<ToolResult> executePlannedTools(AgentRequest request, AgentRunConfig config, AtomicInteger seq,
            List<AgentEvent> events, List<Skill> activeSkills) {
        ToolRequest toolRequest = new ToolRequest(request.message(), config.workspaceRoot(), request.uploadedFileIds(),
                config.threadId());
        List<ToolResult> results = new ArrayList<>();

        List<AgentTool> plannedTools = new ArrayList<>(this.toolRegistry.plan(request.message(), config.maxIterations()));

        // If user has uploaded files, automatically include upload-related tools
        if (request.uploadedFileIds() != null && !request.uploadedFileIds().isEmpty()) {
            for (AgentTool tool : this.toolRegistry.tools()) {
                String name = tool.name();
                if ("read_uploaded_file".equals(name) || "list_uploaded_files".equals(name)) {
                    if (!plannedTools.contains(tool)) {
                        plannedTools.add(tool);
                    }
                }
            }
        }

        for (AgentTool tool : plannedTools) {
            if (this.toolPolicyService != null && !this.toolPolicyService.isToolAllowed(tool.name(), activeSkills)) {
                events.add(event(seq, config, AgentEventType.TOOL_STARTED, "Policy denied " + tool.name(),
                        Map.of("tool", tool.name(), "denied", true)));
                results.add(ToolResult.of(tool.name(), "Tool denied by policy: not in allowed-tools for active skills."));
                events.add(event(seq, config, AgentEventType.TOOL_COMPLETED, "Tool denied by policy",
                        Map.of("tool", tool.name(), "denied", true)));
                continue;
            }
            long toolStartTime = System.currentTimeMillis();
            events.add(event(seq, config, AgentEventType.TOOL_STARTED, "Executing " + tool.name(),
                    Map.of("tool", tool.name(), "description", tool.description())));
            ToolResult result = executeToolSafely(tool, toolRequest);
            long toolDuration = System.currentTimeMillis() - toolStartTime;
            log.info("Tool executed. tool={}, runId={}, durationMs={}, resultLength={}", tool.name(), config.runId(), toolDuration, result.content().length());
            results.add(result);
            events.add(event(seq, config, AgentEventType.TOOL_COMPLETED, result.content(),
                    Map.of("tool", result.toolName(), "durationMs", toolDuration)));
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

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata) {
        return AgentEvent.of(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type, content,
                metadata);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String describeException(Throwable ex) {
        String message = ex.getMessage();
        if (StringUtils.hasText(message)) {
            return ex.getClass().getName() + ": " + message;
        }
        return ex.getClass().getName();
    }
}
