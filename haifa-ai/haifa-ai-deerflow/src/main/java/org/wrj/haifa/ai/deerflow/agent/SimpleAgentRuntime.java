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
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolExecutionStore;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
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
import reactor.core.scheduler.Schedulers;

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
    private final AgentLoop agentLoop;
    private final AgentEventStore agentEventStore;
    private final ToolExecutionStore toolExecutionStore;
    private final ModelStepStore modelStepStore;
    private final ToolCallStore toolCallStore;
    private final AgentLoopRunStore agentLoopRunStore;
    private final org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage;
    private final ResearchRuntimeSupport researchRuntimeSupport;

    @Autowired(required = false)
    private ToolPolicyService toolPolicyService;

    @Autowired(required = false)
    private SlashSkillResolver slashSkillResolver;

    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage) {
        this(properties, toolRegistry, modelClient, runManager, threadManager, messageStore, middlewares,
                agentEventStore, toolExecutionStore, modelStepStore, toolCallStore, agentLoopRunStore, skillStorage, null);
    }

    @Autowired
    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage,
            @Autowired(required = false) ResearchRuntimeSupport researchRuntimeSupport) {
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
        this.agentLoop = new AgentLoop(modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore,
                researchRuntimeSupport);
        this.agentEventStore = agentEventStore;
        this.toolExecutionStore = toolExecutionStore;
        this.modelStepStore = modelStepStore;
        this.toolCallStore = toolCallStore;
        this.agentLoopRunStore = agentLoopRunStore;
        this.skillStorage = skillStorage;
        this.researchRuntimeSupport = researchRuntimeSupport;
    }

    @Override
    public Flux<AgentEvent> stream(AgentRequest request) {
        if (request.isResearchMode()) {
            return streamResearch(request);
        }
        return streamChat(request);
    }

    private Flux<AgentEvent> streamChat(AgentRequest request) {
        return Flux.defer(() -> {
            long runStartTime = System.currentTimeMillis();
            String requestedThreadId = StringUtils.hasText(request.threadId()) ? request.threadId() : UUID.randomUUID().toString();
            ThreadRecord thread = this.threadManager.upsert(requestedThreadId,
                    ThreadManager.titleFromMessage(request.message()), Map.of("source", "run"));
            String threadId = thread.threadId();
            String modelName = StringUtils.hasText(request.model()) ? request.model() : this.properties.getModel();
            int uploadedFileCount = request.uploadedFileIds() == null ? 0 : request.uploadedFileIds().size();
            RunRecord run = this.runManager.create(threadId, modelName, Map.of("source", "sse", "uploadedFiles", uploadedFileCount, "mode", "chat"));
            this.runManager.markRunning(run.runId());
            this.messageStore.add(threadId, run.runId(), MessageRole.USER, request.message(),
                    Map.of("uploadedFileCount", uploadedFileCount));
            log.info("Run started. runId={}, threadId={}, model={}, uploadedFileCount={}, mode=chat", run.runId(), threadId, nullToEmpty(modelName), uploadedFileCount);

            AgentRunConfig config = new AgentRunConfig(threadId, run.runId(), modelName, true, false,
                    this.properties.getMaxIterations(), Path.of(this.properties.getWorkspaceRoot()), RunMode.CHAT,
                    ResearchOptions.defaults(), Map.of("runtime", "simple-agent-runtime"));
            AtomicInteger seq = new AtomicInteger();

            List<AgentEvent> prefixEvents = new ArrayList<>();
            prefixEvents.add(event(seq, config, AgentEventType.RUN_STARTED, "Run started", Map.of("uploadedFileCount", uploadedFileCount, "mode", "chat")));

            List<Skill> activeSkills = resolveActiveSkills(request);
            List<ToolResult> toolResults = List.of();

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

                org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig loopConfig = new org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig(
                        config.maxIterations(),
                        15, // max tool calls in chat mode
                        this.properties.getResearchTimeout(),
                        ResearchOptions.defaults());

                Flux<AgentEvent> loopEvents = this.agentLoop.run(
                        loopConfig,
                        config,
                        prompt.systemPrompt(),
                        prompt.userPrompt(),
                        seq,
                        this.toolPolicyService,
                        activeSkills,
                        request.uploadedFileIds()
                );

                final java.util.concurrent.atomic.AtomicReference<String> lastEventType = new java.util.concurrent.atomic.AtomicReference<>();
                final java.util.concurrent.atomic.AtomicReference<String> lastContent = new java.util.concurrent.atomic.AtomicReference<>("");
                final java.util.concurrent.atomic.AtomicReference<Map<String, Object>> lastMetadata = new java.util.concurrent.atomic.AtomicReference<>(Map.of());

                return loopEvents
                                .doOnNext(evt -> {
                                    lastEventType.set(evt.type().name());
                                    if (evt.type() == AgentEventType.MODEL_COMPLETED || evt.type() == AgentEventType.MODEL_DELTA) {
                                        if (evt.content() != null) {
                                            lastContent.set(evt.content());
                                        }
                                    }
                                    if (evt.metadata() != null) {
                                        lastMetadata.set(evt.metadata());
                                    }
                                     if (evt.type() == AgentEventType.RUN_FAILED) {
                                         this.runManager.markFailed(run.runId(), evt.content());
                                         this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, evt.content(),
                                                 Map.of("status", "FAILED", "errorType", "ModelException"));
                                     }
                                     if (evt.type() == AgentEventType.TOOL_COMPLETED) {
                                         String toolName = (String) evt.metadata().get("toolName");
                                         this.messageStore.add(threadId, run.runId(), MessageRole.TOOL, evt.content(),
                                                 Map.of("tool", toolName != null ? toolName : "unknown",
                                                        "tool_call_id", evt.metadata().getOrDefault("toolCallId", "")));
                                     }
                                 })
                                .doOnComplete(() -> {
                                    String lastType = lastEventType.get();
                                    long totalDuration = System.currentTimeMillis() - runStartTime;
                                    if ("RUN_FAILED".equals(lastType)) {
                                        this.runManager.markFailed(run.runId(), lastContent.get());
                                        this.threadManager.touch(threadId);
                                    } else if ("RUN_CANCELLED".equals(lastType)) {
                                        this.threadManager.touch(threadId);
                                    } else {
                                        this.runManager.markCompleted(run.runId());
                                        this.threadManager.touch(threadId);
                                        String assistantAnswer = lastContent.get();
                                        org.wrj.haifa.ai.deerflow.agent.loop.ToolCallParser parser = new org.wrj.haifa.ai.deerflow.agent.loop.ToolCallParser();
                                        assistantAnswer = parser.cleanResponseText(assistantAnswer);
                                        int toolCount = toolResults.size() + (int) lastMetadata.get().getOrDefault("totalToolCalls", 0);
                                        this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT, assistantAnswer,
                                                Map.of("toolCount", toolCount));
                                    }
                                    log.info("Chat run completed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
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
                                });
            });

            return Flux.concat(Flux.fromIterable(prefixEvents), modelAndTerminalEvents)
                    .doOnNext(this::saveEvent)
                    .doOnCancel(() -> {
                        long totalDuration = System.currentTimeMillis() - runStartTime;
                        log.info("Run cancelled. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        this.runManager.markCancelled(run.runId());
                        this.threadManager.touch(threadId);
                        this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, "Run cancelled",
                                Map.of("status", "CANCELLED", "totalDurationMs", totalDuration));
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<AgentEvent> streamResearch(AgentRequest request) {
        return Flux.defer(() -> {
            long runStartTime = System.currentTimeMillis();
            String requestedThreadId = StringUtils.hasText(request.threadId()) ? request.threadId() : UUID.randomUUID().toString();
            ThreadRecord thread = this.threadManager.upsert(requestedThreadId,
                    ThreadManager.titleFromMessage(request.message()), Map.of("source", "run"));
            String threadId = thread.threadId();
            String modelName = StringUtils.hasText(request.model()) ? request.model() : this.properties.getModel();
            int uploadedFileCount = request.uploadedFileIds() == null ? 0 : request.uploadedFileIds().size();
            ResearchOptions researchOptions = request.researchOptions() == null ? ResearchOptions.defaults() : request.researchOptions();
            Map<String, Object> runMetadata = Map.of(
                    "source", "sse",
                    "uploadedFiles", uploadedFileCount,
                    "mode", "research",
                    "depth", researchOptions.depth().name(),
                    "timeWindow", researchOptions.timeWindow().name(),
                    "maxSources", researchOptions.maxSources(),
                    "requireCitations", researchOptions.requireCitations(),
                    "outputFormat", researchOptions.outputFormat().name()
            );
            RunRecord run = this.runManager.create(threadId, modelName, runMetadata);
            this.runManager.markRunning(run.runId());
            this.messageStore.add(threadId, run.runId(), MessageRole.USER, request.message(),
                    Map.of("uploadedFileCount", uploadedFileCount, "mode", "research"));
            log.info("Research run started. runId={}, threadId={}, model={}, depth={}, timeWindow={}, maxSources={}, outputFormat={}",
                    run.runId(), threadId, nullToEmpty(modelName), researchOptions.depth(), researchOptions.timeWindow(),
                    researchOptions.maxSources(), researchOptions.outputFormat());

            AgentRunConfig config = new AgentRunConfig(threadId, run.runId(), modelName, true, false,
                    this.properties.getMaxIterations(), Path.of(this.properties.getWorkspaceRoot()), RunMode.RESEARCH,
                    researchOptions, Map.of("runtime", "agent-loop"));
            AtomicInteger seq = new AtomicInteger();

            List<AgentEvent> prefixEvents = new ArrayList<>();
            prefixEvents.add(event(seq, config, AgentEventType.RUN_STARTED, "Research run started", runMetadata));
            prefixEvents.add(event(seq, config, AgentEventType.RESEARCH_PLAN_CREATED,
                    "Research plan created for: " + request.message(),
                    Map.of("depth", researchOptions.depth().name(), "timeWindow", researchOptions.timeWindow().name(),
                            "maxSources", researchOptions.maxSources(), "outputFormat", researchOptions.outputFormat().name())));

            List<Skill> activeSkills = new java.util.ArrayList<>(resolveActiveSkills(request));
            boolean hasDeepResearch = activeSkills.stream().anyMatch(s -> "deep-research".equals(s.name()));
            if (!hasDeepResearch) {
                this.skillStorage.findAny("deep-research").ifPresent(activeSkills::add);
            }

            List<Skill> availableSkills = this.skillStorage.listAll();
            String skillsSection = org.wrj.haifa.ai.deerflow.skill.SkillPromptRenderer.renderSkillSystem(
                    availableSkills,
                    activeSkills,
                    this.properties.getSkillsRoot()
            );

            String uploadsPath = this.properties.getUploadsRoot();
            String workspacePath = this.properties.getWorkspaceRoot();
            String outputsPath = this.properties.getOutputsRoot();

            String customResearchSystemPrompt = this.properties.getResearchSystemPrompt() != null
                    ? this.properties.getResearchSystemPrompt()
                    : this.properties.getSystemPrompt();

            String researchSystemPrompt = org.wrj.haifa.ai.deerflow.prompt.ResearchPromptTemplate.build(
                    modelName,
                    researchOptions,
                    skillsSection,
                    uploadsPath,
                    workspacePath,
                    outputsPath,
                    customResearchSystemPrompt
            );

            LoopConfig loopConfig = new LoopConfig(
                    this.properties.getMaxResearchSteps(),
                    this.properties.getMaxFetchesPerRun(),
                    this.properties.getResearchTimeout(),
                    researchOptions);

            // Run the agent loop with full context (policy, skills, uploads)
            Flux<AgentEvent> loopEvents = this.agentLoop.run(loopConfig, config, researchSystemPrompt, request.message(), seq,
                    this.toolPolicyService, activeSkills, request.uploadedFileIds());

            // After loop completes, mark run status based on last event type
            final java.util.concurrent.atomic.AtomicReference<String> lastEventType = new java.util.concurrent.atomic.AtomicReference<>();
            Flux<AgentEvent> wrappedLoopEvents = loopEvents
                    .doOnNext(evt -> {
                        lastEventType.set(evt.type().name());
                        if (evt.type() == AgentEventType.MODEL_COMPLETED) {
                            this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT, evt.content(),
                                    Map.of("mode", "research", "toolCount", evt.metadata().getOrDefault("totalToolCalls", 0)));
                        }
                    })
                    .doOnComplete(() -> {
                        String lastType = lastEventType.get();
                        long totalDuration = System.currentTimeMillis() - runStartTime;
                        if ("RUN_FAILED".equals(lastType)) {
                            // Already marked as failed by AgentLoop — do not override
                            this.threadManager.touch(threadId);
                            log.info("Research run failed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        } else if ("RUN_CANCELLED".equals(lastType)) {
                            this.threadManager.touch(threadId);
                            log.info("Research run cancelled. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        } else {
                            this.runManager.markCompleted(run.runId());
                            this.threadManager.touch(threadId);
                            log.info("Research run completed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        }
                    })
                    .doOnError(ex -> {
                        String errorMessage = describeException(ex);
                        this.runManager.markFailed(run.runId(), errorMessage);
                        this.threadManager.touch(threadId);
                        this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, errorMessage,
                                Map.of("status", "FAILED", "errorType", ex.getClass().getName(), "mode", "research"));
                        long totalDuration = System.currentTimeMillis() - runStartTime;
                        log.error("Research run failed. runId={}, totalDurationMs={}", run.runId(), totalDuration, ex);
                    })
                    .doOnCancel(() -> {
                        long totalDuration = System.currentTimeMillis() - runStartTime;
                        log.info("Research run cancelled. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        this.runManager.markCancelled(run.runId());
                        this.threadManager.touch(threadId);
                        this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, "Research run cancelled",
                                Map.of("status", "CANCELLED", "totalDurationMs", totalDuration, "mode", "research"));
                    });

            return Flux.concat(Flux.fromIterable(prefixEvents), wrappedLoopEvents)
                    .doOnNext(this::saveEvent);
        }).subscribeOn(Schedulers.boundedElastic());
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
                this.toolExecutionStore.saveDenied(config.runId(), config.threadId(), tool.name(),
                        "Policy denied: not in allowed-tools for active skills",
                        Map.of("tool", tool.name(), "denied", true));
                continue;
            }
            long toolStartTime = System.currentTimeMillis();
            events.add(event(seq, config, AgentEventType.TOOL_STARTED, "Executing " + tool.name(),
                    Map.of("tool", tool.name(), "description", tool.description())));
            this.toolExecutionStore.saveStarted(config.runId(), config.threadId(), tool.name(), tool.description(),
                    toolRequest.toString(), Map.of("tool", tool.name()));
            ToolResult result = executeToolSafely(tool, toolRequest);
            long toolDuration = System.currentTimeMillis() - toolStartTime;
            log.info("Tool executed. tool={}, runId={}, durationMs={}, resultLength={}", tool.name(), config.runId(), toolDuration, result.content().length());
            results.add(result);
            events.add(event(seq, config, AgentEventType.TOOL_COMPLETED, result.content(),
                    Map.of("tool", result.toolName(), "durationMs", toolDuration)));
            if (result.content() != null && result.content().startsWith("Tool failed:")) {
                this.toolExecutionStore.saveFailed(config.runId(), config.threadId(), tool.name(), result.content(),
                        toolDuration, Map.of("tool", tool.name()));
            } else {
                this.toolExecutionStore.saveCompleted(config.runId(), config.threadId(), tool.name(), result.content(),
                        toolDuration, Map.of("tool", tool.name()));
            }
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

    private void saveEvent(AgentEvent event) {
        try {
            this.agentEventStore.save(event);
            if (event.type() == AgentEventType.TOOL_STARTED) {
                String toolName = (String) event.metadata().get("toolName");
                if (toolName == null) toolName = (String) event.metadata().get("tool");
                String desc = (String) event.metadata().getOrDefault("description", "");
                Boolean denied = (Boolean) event.metadata().getOrDefault("denied", false);
                if (Boolean.TRUE.equals(denied)) {
                    this.toolExecutionStore.saveDenied(event.runId(), event.threadId(), toolName, "Policy denied", event.metadata());
                } else {
                    String reqStr = (String) event.metadata().getOrDefault("arguments", "");
                    this.toolExecutionStore.saveStarted(event.runId(), event.threadId(), toolName, desc, reqStr, event.metadata());
                }
            } else if (event.type() == AgentEventType.TOOL_COMPLETED) {
                String toolName = (String) event.metadata().get("toolName");
                if (toolName == null) toolName = (String) event.metadata().get("tool");
                Boolean denied = (Boolean) event.metadata().getOrDefault("denied", false);
                if (!Boolean.TRUE.equals(denied)) {
                    long duration = ((Number) event.metadata().getOrDefault("durationMs", 0L)).longValue();
                    String status = (String) event.metadata().getOrDefault("status", "COMPLETED");
                    if ("FAILED".equals(status) || (event.content() != null && event.content().startsWith("Tool failed:"))) {
                        this.toolExecutionStore.saveFailed(event.runId(), event.threadId(), toolName, event.content(), duration, event.metadata());
                    } else {
                        this.toolExecutionStore.saveCompleted(event.runId(), event.threadId(), toolName, event.content(), duration, event.metadata());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to persist event or execution: {}", e.getMessage());
        }
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
