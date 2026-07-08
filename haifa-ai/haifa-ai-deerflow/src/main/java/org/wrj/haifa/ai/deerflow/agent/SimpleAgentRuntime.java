package org.wrj.haifa.ai.deerflow.agent;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.CompositeAgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.DefaultAgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriteResult;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriterService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.config.GraphRuntimeMode;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntime;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntimeRequest;
import org.wrj.haifa.ai.deerflow.graph.GraphResearchRuntime;
import org.wrj.haifa.ai.deerflow.graph.GraphResearchRuntimeRequest;
import org.wrj.haifa.ai.deerflow.graph.GraphShadowRuntime;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder;
import org.wrj.haifa.ai.deerflow.middleware.SubagentLimitMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolExecutionStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyService;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
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
    private final ClarificationStore clarificationStore;
    private final ReportWriterService reportWriterService;
    private final TodoStore todoStore;
    private final ApprovalPolicyService approvalPolicyService;
    private final ApprovalStore approvalStore;

    @Autowired(required = false)
    private ToolPolicyService toolPolicyService;

    @Autowired(required = false)
    private SlashSkillResolver slashSkillResolver;

    @Autowired(required = false)
    private org.wrj.haifa.ai.deerflow.memory.MemoryReflectionService memoryReflectionService;

    @Autowired(required = false)
    private GraphShadowRuntime graphShadowRuntime;

    @Autowired(required = false)
    private org.wrj.haifa.ai.deerflow.artifact.ArtifactService artifactService;

    @Autowired(required = false)
    private GraphChatRuntime graphChatRuntime;

    @Autowired(required = false)
    private GraphResearchRuntime graphResearchRuntime;

    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage) {
        this(properties, toolRegistry, modelClient, runManager, threadManager, messageStore, middlewares,
                agentEventStore, toolExecutionStore, modelStepStore, toolCallStore, agentLoopRunStore, skillStorage,
                null, null, null, null, null, null, null, null);
    }

    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage,
            TodoStore todoStore,
            ToolOutputBudgetMiddleware toolOutputBudgetMiddleware,
            ResearchRuntimeSupport researchRuntimeSupport,
            ClarificationStore clarificationStore,
            ReportWriterService reportWriterService,
            SubagentLimitMiddleware subagentLimitMiddleware) {
        this(properties, toolRegistry, modelClient, runManager, threadManager, messageStore, middlewares,
                agentEventStore, toolExecutionStore, modelStepStore, toolCallStore, agentLoopRunStore, skillStorage,
                todoStore, toolOutputBudgetMiddleware, researchRuntimeSupport, clarificationStore, reportWriterService,
                subagentLimitMiddleware, null, null);
    }

    @Autowired
    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage,
            @Autowired(required = false) TodoStore todoStore,
            @Autowired(required = false) ToolOutputBudgetMiddleware toolOutputBudgetMiddleware,
            @Autowired(required = false) ResearchRuntimeSupport researchRuntimeSupport,
            @Autowired(required = false) ClarificationStore clarificationStore,
            @Autowired(required = false) ReportWriterService reportWriterService,
            @Autowired(required = false) SubagentLimitMiddleware subagentLimitMiddleware,
            @Autowired(required = false) ApprovalPolicyService approvalPolicyService,
            @Autowired(required = false) ApprovalStore approvalStore) {
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

        List<org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver> observers = new ArrayList<>();
        observers.add(new DefaultAgentLoopObserver(todoStore, artifactService));
        if (subagentLimitMiddleware != null) {
            observers.add(subagentLimitMiddleware);
        }

        this.agentLoop = new AgentLoop(modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore,
                new CompositeAgentLoopObserver(observers),
                toolOutputBudgetMiddleware,
                clarificationStore,
                approvalPolicyService,
                approvalStore);
        this.agentEventStore = agentEventStore;
        this.toolExecutionStore = toolExecutionStore;
        this.modelStepStore = modelStepStore;
        this.toolCallStore = toolCallStore;
        this.agentLoopRunStore = agentLoopRunStore;
        this.skillStorage = skillStorage;
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.clarificationStore = clarificationStore;
        this.reportWriterService = reportWriterService;
        this.todoStore = todoStore;
        this.approvalPolicyService = approvalPolicyService;
        this.approvalStore = approvalStore;
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
            ResearchOptions researchOptions = request.researchOptions() == null
                    ? ResearchOptions.defaults() : request.researchOptions();
            AgentRequest effectiveRequest = request;

            Map<String, Object> runMetadata = new HashMap<>();
            runMetadata.put("source", "sse");
            runMetadata.put("uploadedFiles", uploadedFileCount);
            runMetadata.put("mode", request.isResearchMode() ? "research" : "chat");
            runMetadata.put("userId", request.userId());
            if (request.metadata() != null) {
                runMetadata.putAll(request.metadata());
            }
            if (request.isResearchMode()) {
                runMetadata.put("depth", researchOptions.depth().name());
                runMetadata.put("timeWindow", researchOptions.timeWindow().name());
                runMetadata.put("maxSources", researchOptions.maxSources());
                runMetadata.put("requireCitations", researchOptions.requireCitations());
                runMetadata.put("outputFormat", researchOptions.outputFormat().name());
            }
            Map<String, Object> runMetadataCopy = Map.copyOf(runMetadata);
            RunRecord run = this.runManager.create(threadId, modelName, runMetadataCopy);
            this.runManager.markRunning(run.runId());

            String logMessage = request.message();
            if (request.metadata() != null && request.metadata().containsKey("clarificationId") && this.clarificationStore != null) {
                String clarId = (String) request.metadata().get("clarificationId");
                logMessage = this.clarificationStore.find(clarId)
                        .map(org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord::answer)
                        .orElse(request.message());
            }
            this.messageStore.add(threadId, run.runId(), MessageRole.USER, logMessage,
                    Map.of("uploadedFileCount", uploadedFileCount, "mode", request.isResearchMode() ? "research" : "chat"));
            log.info("Run started. runId={}, threadId={}, model={}, uploadedFileCount={}, mode={}",
                    run.runId(), threadId, nullToEmpty(modelName), uploadedFileCount, runMetadata.get("mode"));

            Map<String, Object> configMetadata = new HashMap<>();
            configMetadata.put("runtime", "simple-agent-runtime");
            configMetadata.put("userId", request.userId());
            if (request.metadata() != null) {
                configMetadata.putAll(request.metadata());
            }


            RunMode runMode = request.mode();
            AgentRunConfig config = new AgentRunConfig(
                    threadId, run.runId(), modelName, true, false,
                    this.properties.getMaxIterations(),
                    Path.of(this.properties.getWorkspaceRoot()),
                    runMode,
                    researchOptions,
                    configMetadata
            );

            AtomicInteger seq = new AtomicInteger();
            List<AgentEvent> prefixEvents = new ArrayList<>();
            prefixEvents.add(event(seq, config, AgentEventType.RUN_STARTED,
                    request.isResearchMode() ? "Research run started" : "Run started",
                    runMetadataCopy));

            List<Skill> activeSkills = new ArrayList<>(resolveActiveSkills(effectiveRequest));
            if (request.isResearchMode()) {
                boolean hasDeepResearch = activeSkills.stream().anyMatch(s -> "deep-research".equals(s.name()));
                if (!hasDeepResearch) {
                    this.skillStorage.findAny("deep-research").ifPresent(activeSkills::add);
                }
            }

            List<ToolResult> toolResults = List.of();
            AgentRuntimeContext runtimeContext = AgentRuntimeContext.of(config, effectiveRequest, toolResults, this.properties, activeSkills);
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
                    return Flux.fromIterable(List.of(
                            event(seq, config, AgentEventType.MODEL_COMPLETED,
                                    "Budget exceeded: request + observations too large.",
                                    Map.of("budgetExceeded", true, "toolCount", toolResults.size())),
                            event(seq, config, AgentEventType.RUN_COMPLETED, "Run completed with budget limit",
                                    Map.of("status", "BUDGET_LIMITED", "totalDurationMs", totalDuration))
                    ));
                }

                int maxSteps = request.isResearchMode() ? this.properties.getMaxResearchSteps() : config.maxIterations();
                int maxToolCalls = request.isResearchMode() ? this.properties.getMaxFetchesPerRun() : this.properties.getMaxToolCalls();
                LoopConfig loopConfig = new LoopConfig(
                        maxSteps, maxToolCalls, this.properties.getResearchTimeout(), researchOptions);

                boolean activeChatGraph = shouldUseActiveChatGraph(this.properties, this.graphChatRuntime, effectiveRequest);
                boolean activeResearchGraph = shouldUseActiveResearchGraph(this.properties, this.graphResearchRuntime, effectiveRequest);
                if (!activeChatGraph && !activeResearchGraph) {
                    triggerGraphShadow(config, effectiveRequest, prompt);
                }

                Flux<AgentEvent> loopEvents = activeChatGraph
                        ? this.graphChatRuntime.run(new GraphChatRuntimeRequest(
                                this.agentLoop,
                                loopConfig,
                                config,
                                effectiveRequest,
                                prompt.systemPrompt(),
                                prompt.userPrompt(),
                                seq,
                                this.toolPolicyService,
                                activeSkills,
                                request.uploadedFileIds(),
                                this.messageStore.listByThread(config.threadId())
                        ))
                        : activeResearchGraph
                                ? this.graphResearchRuntime.run(new GraphResearchRuntimeRequest(
                                        this.agentLoop,
                                        loopConfig,
                                        config,
                                        effectiveRequest,
                                        prompt.systemPrompt(),
                                        prompt.userPrompt(),
                                        seq,
                                        this.toolPolicyService,
                                        activeSkills,
                                        request.uploadedFileIds(),
                                        this.messageStore.listByThread(config.threadId())
                                ))
                                : this.agentLoop.run(
                                        loopConfig, config,
                                        prompt.systemPrompt(), prompt.userPrompt(),
                                        seq, this.toolPolicyService, activeSkills, request.uploadedFileIds());

                final AtomicReference<String> lastEventType = new AtomicReference<>();
                final AtomicReference<String> lastContent = new AtomicReference<>("");
                final AtomicReference<Map<String, Object>> lastMetadata = new AtomicReference<>(Map.of());

                Flux<AgentEvent> wrappedLoopEvents = loopEvents
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
                            if (evt.type() == AgentEventType.MODEL_DELTA
                                    && Boolean.TRUE.equals(evt.metadata().get("persistAssistantToolCalls"))) {
                                Object toolCalls = evt.metadata().get("tool_calls");
                                java.util.Map<String, Object> assistantMetadata = new java.util.HashMap<>();
                                assistantMetadata.put("tool_calls", toolCalls == null ? List.of() : toolCalls);
                                assistantMetadata.put("step", evt.metadata().getOrDefault("step", 0));
                                assistantMetadata.put("modelDurationMs", evt.metadata().getOrDefault("modelDurationMs", 0));
                                assistantMetadata.put("intermediate", true);
                                this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT,
                                        evt.content() == null ? "" : evt.content(),
                                        assistantMetadata);
                            }
                            if (evt.type() == AgentEventType.TOOL_COMPLETED || evt.type() == AgentEventType.TOOL_DENIED) {
                                String toolName = (String) evt.metadata().get("toolName");
                                if (toolName == null) toolName = (String) evt.metadata().get("tool");
                                this.messageStore.add(threadId, run.runId(), MessageRole.TOOL, evt.content(),
                                        Map.of("tool", toolName != null ? toolName : "unknown",
                                                "tool_call_id", evt.metadata().getOrDefault("toolCallId", "")));
                            }
                            if (evt.type() == AgentEventType.CLARIFICATION_REQUIRED) {
                                String question = (String) evt.metadata().get("question");
                                String clarificationId = (String) evt.metadata().get("clarificationId");
                                Object options = evt.metadata().get("options");
                                Object questions = evt.metadata().get("questions");
                                java.util.Map<String, Object> meta = new java.util.HashMap<>();
                                meta.put("clarificationPending", true);
                                meta.put("question", question);
                                meta.put("clarificationId", clarificationId != null ? clarificationId : "");
                                meta.put("resumeThreadId", threadId);
                                meta.put("resumeRunId", run.runId());
                                if (options != null) {
                                    meta.put("options", options);
                                }
                                if (questions != null) {
                                    meta.put("questions", questions);
                                }
                                this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM,
                                        "Clarification needed: " + question,
                                        meta);
                                this.runManager.markSuspended(run.runId());
                                this.threadManager.touch(threadId);
                            }
                            if (evt.type() == AgentEventType.APPROVAL_REQUIRED) {
                                String reason = (String) evt.metadata().get("reason");
                                String approvalId = (String) evt.metadata().get("approvalId");
                                this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM,
                                        "Approval needed: " + reason,
                                        Map.of("approvalPending", true, "approvalId", approvalId,
                                                "resumeThreadId", threadId, "resumeRunId", run.runId()));
                                this.runManager.markSuspended(run.runId());
                                this.threadManager.touch(threadId);
                            }
                        });

                if (request.isResearchMode()) {
                    final AtomicReference<String> lastModelContent = new AtomicReference<>("");
                    final AtomicReference<Map<String, Object>> lastModelMetadata = new AtomicReference<>(Map.of());
                    Flux<AgentEvent> mappedLoopEvents = wrappedLoopEvents.map(evt -> {
                        if (evt.type() == AgentEventType.MODEL_COMPLETED) {
                            lastModelContent.set(evt.content() == null ? "" : evt.content());
                            lastModelMetadata.set(evt.metadata() == null ? Map.of() : evt.metadata());
                            return event(seq, config, AgentEventType.MODEL_COMPLETED,
                                    "Research synthesis captured. Preparing report artifact.",
                                    Map.of("mode", "research", "reportPending", true,
                                            "stopReason", evt.metadata().getOrDefault("stopReason", "")));
                        }
                        return evt;
                    });

                    Flux<AgentEvent> deliveryEvents = Flux.defer(() -> Flux.fromIterable(finishResearchDelivery(
                            lastEventType.get(),
                            run,
                            threadId,
                            config,
                            seq,
                            runStartTime,
                            lastModelContent.get(),
                            lastModelMetadata.get()
                    )));

                    return Flux.concat(mappedLoopEvents, deliveryEvents)
                            .doOnComplete(() -> {
                                String lastType = lastEventType.get();
                                long totalDuration = System.currentTimeMillis() - runStartTime;
                                if ("RUN_FAILED".equals(lastType)) {
                                    this.runManager.markFailed(run.runId(), lastContent.get());
                                    this.threadManager.touch(threadId);
                                } else if ("RUN_CANCELLED".equals(lastType)) {
                                    this.threadManager.touch(threadId);
                                } else if ("CLARIFICATION_REQUIRED".equals(lastType) || "APPROVAL_REQUIRED".equals(lastType) || "RUN_SUSPENDED".equals(lastType)) {
                                    // already handled in doOnNext
                                } else {
                                    this.runManager.markCompleted(run.runId());
                                    this.threadManager.touch(threadId);
                                    if (this.memoryReflectionService != null) {
                                        this.memoryReflectionService.reflectAsync(threadId, run.runId());
                                    }
                                }
                                log.info("Research run completed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                            })
                            .onErrorResume(ex -> {
                                String errorMessage = describeException(ex);
                                long totalDuration = System.currentTimeMillis() - runStartTime;
                                log.error("Research run failed. runId={}, totalDurationMs={}", run.runId(), totalDuration, ex);
                                this.runManager.markFailed(run.runId(), errorMessage);
                                this.threadManager.touch(threadId);
                                this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, errorMessage,
                                        Map.of("status", "FAILED", "errorType", ex.getClass().getName(), "mode", "research"));
                                return Flux.fromIterable(List.of(
                                        event(seq, config, AgentEventType.RUN_FAILED, errorMessage,
                                                Map.of("status", "FAILED", "errorType", ex.getClass().getName(), "totalDurationMs", totalDuration))
                                ));
                            });
                }

                return wrappedLoopEvents
                        .doOnComplete(() -> {
                            String lastType = lastEventType.get();
                            long totalDuration = System.currentTimeMillis() - runStartTime;
                            if ("RUN_FAILED".equals(lastType)) {
                                this.runManager.markFailed(run.runId(), lastContent.get());
                                this.threadManager.touch(threadId);
                            } else if ("RUN_CANCELLED".equals(lastType)) {
                                this.threadManager.touch(threadId);
                            } else if ("CLARIFICATION_REQUIRED".equals(lastType) || "APPROVAL_REQUIRED".equals(lastType) || "RUN_SUSPENDED".equals(lastType)) {
                                // already handled in doOnNext
                            } else {
                                this.runManager.markCompleted(run.runId());
                                this.threadManager.touch(threadId);
                                String assistantAnswer = lastContent.get();
                                int toolCount = (int) lastMetadata.get().getOrDefault("totalToolCalls", 0);
                                this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT, assistantAnswer,
                                        Map.of("toolCount", toolCount, "mode", "chat"));
                                if (this.memoryReflectionService != null) {
                                    this.memoryReflectionService.reflectAsync(threadId, run.runId());
                                }
                            }
                            log.info("Chat run completed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
                        })
                        .onErrorResume(ex -> {
                            String errorMessage = describeException(ex);
                            long totalDuration = System.currentTimeMillis() - runStartTime;
                            log.error("DeerFlow run failed during model generation. runId={}, threadId={}, model={}, totalDurationMs={}",
                                    run.runId(), threadId, nullToEmpty(modelName), totalDuration, ex);
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
                    .onErrorResume(ex -> {
                        String errorMessage = describeException(ex);
                        long totalDuration = System.currentTimeMillis() - runStartTime;
                        log.error("DeerFlow run failed before terminal event. runId={}, threadId={}, model={}, totalDurationMs={}",
                                run.runId(), threadId, nullToEmpty(modelName), totalDuration, ex);
                        this.runManager.markFailed(run.runId(), errorMessage);
                        this.threadManager.touch(threadId);
                        this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM, errorMessage,
                                Map.of("status", "FAILED", "errorType", ex.getClass().getName()));
                        return Flux.just(event(seq, config, AgentEventType.RUN_FAILED, errorMessage,
                                Map.of("status", "FAILED", "errorType", ex.getClass().getName(),
                                        "totalDurationMs", totalDuration)));
                    })
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

    void setGraphShadowRuntime(GraphShadowRuntime graphShadowRuntime) {
        this.graphShadowRuntime = graphShadowRuntime;
    }

    void setGraphChatRuntime(GraphChatRuntime graphChatRuntime) {
        this.graphChatRuntime = graphChatRuntime;
    }

    void setGraphResearchRuntime(GraphResearchRuntime graphResearchRuntime) {
        this.graphResearchRuntime = graphResearchRuntime;
    }

    public static boolean shouldUseActiveChatGraph(DeerFlowProperties properties, GraphChatRuntime graphChatRuntime, AgentRequest request) {
        return graphChatRuntime != null
                && properties.getGraph() != null
                && properties.getGraph().isEnabled()
                && properties.getGraph().getMode() == GraphRuntimeMode.ACTIVE_CHAT
                && request != null
                && request.isChatMode();
    }

    public static boolean shouldUseActiveResearchGraph(DeerFlowProperties properties, GraphResearchRuntime graphResearchRuntime, AgentRequest request) {
        return graphResearchRuntime != null
                && properties.getGraph() != null
                && properties.getGraph().isEnabled()
                && properties.getGraph().getMode() == GraphRuntimeMode.ACTIVE_RESEARCH
                && request != null
                && request.isResearchMode();
    }

    private void triggerGraphShadow(AgentRunConfig config, AgentRequest request, ModelPrompt prompt) {
        if (this.graphShadowRuntime == null
                || this.properties.getGraph() == null
                || !this.properties.getGraph().isEnabled()
                || this.properties.getGraph().getMode() != GraphRuntimeMode.SHADOW
                || request == null
                || !request.isChatMode()) {
            return;
        }
        this.graphShadowRuntime.run(config, request, this.messageStore.listByThread(config.threadId()), prompt)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> log.debug("Graph shadow completed. runId={}, threadId={}, nodes={}, durationMs={}",
                        result.runId(), result.threadId(), result.visitedNodes(), result.durationMs()))
                .doOnError(ex -> log.warn("Graph shadow failed without affecting legacy runtime. runId={}",
                        config.runId(), ex))
                .subscribe();
    }

    private List<AgentEvent> finishResearchDelivery(String lastType, RunRecord run, String threadId,
            AgentRunConfig config, AtomicInteger seq, long runStartTime, String synthesisResult,
            Map<String, Object> modelMetadata) {
        if ("RUN_FAILED".equals(lastType) || "RUN_CANCELLED".equals(lastType)
                || "CLARIFICATION_REQUIRED".equals(lastType) || "RUN_SUSPENDED".equals(lastType)) {
            return List.of();
        }
        long totalDuration = System.currentTimeMillis() - runStartTime;

        List<AgentEvent> events = new ArrayList<>();
        List<ResearchSource> sources = this.researchRuntimeSupport == null
                ? List.of()
                : this.researchRuntimeSupport.listSourcesByRun(run.runId());
        List<EvidenceItem> evidenceItems = this.researchRuntimeSupport == null
                ? List.of()
                : this.researchRuntimeSupport.listEvidenceByRun(run.runId());

        ResearchPlan todoPlan = buildTodoBackedResearchPlan(threadId, run.runId());

        if (this.reportWriterService != null) {
            events.add(event(seq, config, AgentEventType.REPORT_STARTED,
                    "Research report generation started", Map.of()));
            ReportWriteResult result = this.reportWriterService.writeReport(
                    threadId, run.runId(), todoPlan, sources, evidenceItems, null,
                    synthesisResult, config.researchOptions());
            events.add(event(seq, config, AgentEventType.ARTIFACT_CREATED,
                    "Research report artifact created: " + result.artifact().filename(),
                    Map.of("artifactId", result.artifact().artifactId(),
                            "filename", result.artifact().filename(),
                            "mimeType", result.artifact().mimeType(),
                            "size", result.artifact().size(),
                            "downloadUrl", "/api/deerflow/artifacts/" + result.artifact().artifactId() + "/download")));
            events.add(event(seq, config, AgentEventType.REPORT_COMPLETED,
                    "Research report completed: " + result.artifact().filename(),
                    Map.of("artifactId", result.artifact().artifactId(),
                            "filename", result.artifact().filename(),
                            "previewUrl", "/api/deerflow/artifacts/" + result.artifact().artifactId(),
                            "downloadUrl", "/api/deerflow/artifacts/" + result.artifact().artifactId() + "/download")));
            this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT,
                    buildArtifactSummary(result, null),
                    Map.of("mode", "research", "artifactId", result.artifact().artifactId(),
                            "filename", result.artifact().filename(),
                            "toolCount", modelMetadata == null ? 0 : modelMetadata.getOrDefault("totalToolCalls", 0)));
        } else {
            this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT,
                    compactResearchAnswer(synthesisResult, null),
                    Map.of("mode", "research", "artifactUnavailable", true));
        }

        this.runManager.markCompleted(run.runId());
        this.threadManager.touch(threadId);
        if (this.memoryReflectionService != null) {
            this.memoryReflectionService.reflectAsync(threadId, run.runId());
        }
        log.info("Research run completed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
        return events;
    }

    private ResearchPlan buildTodoBackedResearchPlan(String threadId, String runId) {
        if (this.todoStore == null) {
            return null;
        }
        List<TodoItem> todos = this.todoStore.listTodos(threadId, runId);
        if (todos.isEmpty()) {
            return null;
        }
        List<ResearchDimension> dimensions = todos.stream()
                .map(todo -> new ResearchDimension(
                        StringUtils.hasText(todo.getId()) ? todo.getId() : UUID.randomUUID().toString(),
                        StringUtils.hasText(todo.getContent()) ? todo.getContent() : "Research task",
                        "Research dimension derived from TodoList item.",
                        toResearchTaskStatus(todo.getStatus()),
                        List.of(),
                        0,
                        0,
                        0,
                        List.of()))
                .toList();
        String topic = todos.stream()
                .map(TodoItem::getContent)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("research");
        Instant now = Instant.now();
        return new ResearchPlan(
                "todo-plan-" + runId,
                threadId,
                runId,
                topic,
                List.of(topic),
                dimensions,
                List.of(),
                "Auto-generated from write_todos results.",
                "Research report",
                "TODO_DRIVEN",
                now,
                now
        );
    }

    private static ResearchTaskStatus toResearchTaskStatus(String status) {
        if ("completed".equalsIgnoreCase(status)) {
            return ResearchTaskStatus.COMPLETED;
        }
        if ("in_progress".equalsIgnoreCase(status)) {
            return ResearchTaskStatus.IN_PROGRESS;
        }
        return ResearchTaskStatus.PENDING;
    }



    private static String buildArtifactSummary(ReportWriteResult result, QualityGateResult qgResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summary\n\n").append(result.summary())
          .append("\n\nLimitations\n\n").append(result.limitations());

        List<String> suggestions = generateSuggestions(qgResult);
        sb.append("\n\nFollow-up Suggestions\n\n");
        for (String sug : suggestions) {
            sb.append("- ").append(sug).append("\n");
        }

        sb.append("\n\nArtifact\n\n- [").append(result.artifact().filename())
          .append("](/api/deerflow/artifacts/").append(result.artifact().artifactId()).append("/download)");
        return sb.toString();
    }

    private static String compactResearchAnswer(String synthesisResult, QualityGateResult qualityGateResult) {
        String answer = synthesisResult == null ? "" : synthesisResult.trim();
        if (answer.length() > 900) {
            answer = answer.substring(0, 900).trim() + "...";
        }
        String limitations = qualityGateResult == null || qualityGateResult.passed()
                ? "No blocking quality gate gaps were recorded."
                : String.join("; ", qualityGateResult.gaps());
        StringBuilder sb = new StringBuilder();
        sb.append("Summary\n\n").append(answer.isBlank() ? "Research completed." : answer)
          .append("\n\nLimitations\n\n").append(limitations);

        List<String> suggestions = generateSuggestions(qualityGateResult);
        sb.append("\n\nFollow-up Suggestions\n\n");
        for (String sug : suggestions) {
            sb.append("- ").append(sug).append("\n");
        }
        return sb.toString();
    }

    private static List<String> generateSuggestions(QualityGateResult qgResult) {
        List<String> suggestions = new ArrayList<>();
        if (qgResult == null || qgResult.passed()) {
            suggestions.add("Expand the research to other related technologies or industries.");
            suggestions.add("Compare these findings with a historical or geographical case study.");
            return suggestions;
        }
        for (String gap : qgResult.gaps()) {
            String lower = gap.toLowerCase();
            if (lower.contains("source") || lower.contains("read")) {
                suggestions.add("Fetch and read more authoritative articles on the topic.");
            } else if (lower.contains("data") || lower.contains("fact") || lower.contains("number")) {
                suggestions.add("Search specifically for quantitative data, statistics, and industry reports.");
            } else if (lower.contains("case") || lower.contains("example")) {
                suggestions.add("Search for real-world case studies or implementation examples.");
            } else if (lower.contains("opinion") || lower.contains("view")) {
                suggestions.add("Seek out expert analyses, interviews, or professional commentaries.");
            } else if (lower.contains("limitation") || lower.contains("gap")) {
                suggestions.add("Investigate the challenges, failures, and limitations of this area.");
            } else if (lower.contains("counter") || lower.contains("oppose") || lower.contains("bias")) {
                suggestions.add("Look into alternative perspectives, critiques, or counterarguments.");
            } else if (lower.contains("citation") || lower.contains("cite")) {
                suggestions.add("Validate the sources and check if all claims have proper inline citations.");
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Expand the research to other related technologies or industries.");
            suggestions.add("Compare these findings with a historical or geographical case study.");
        }
        return suggestions;
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
                config.threadId(), config.runId());
        List<ToolResult> results = new ArrayList<>();

        List<AgentTool> plannedTools = new ArrayList<>(this.toolRegistry.plan(request.message(), config.maxIterations()));

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
            ToolPolicyDecision decision = this.toolPolicyService == null
                    ? ToolPolicyDecision.allow()
                    : this.toolPolicyService.evaluateTool(tool.name(), activeSkills, config.mode());
            if (!decision.allowed()) {
                events.add(event(seq, config, AgentEventType.TOOL_STARTED, "Policy denied " + tool.name(),
                        Map.of("tool", tool.name(), "denied", true)));
                String deniedMessage = "Tool denied by policy: " + decision.reason();
                results.add(ToolResult.of(tool.name(), deniedMessage));
                events.add(event(seq, config, AgentEventType.TOOL_DENIED, deniedMessage,
                        Map.of("tool", tool.name(), "denied", true, "reason", decision.reason())));
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
            // Do not save intermediate MODEL_DELTA streaming chunks to the database to prevent database bloat and write lock issues
            if (event.type() == AgentEventType.MODEL_DELTA) {
                Map<String, Object> meta = event.metadata();
                boolean isFinal = meta != null && (meta.containsKey("modelDurationMs") || meta.containsKey("persistAssistantToolCalls"));
                if (!isFinal) {
                    return;
                }
            }
            this.agentEventStore.save(event);
            if (event.type() == AgentEventType.TOOL_STARTED) {
                String toolName = (String) event.metadata().get("toolName");
                if (toolName == null) toolName = (String) event.metadata().get("tool");
                String desc = (String) event.metadata().getOrDefault("description", "");
                Boolean denied = (Boolean) event.metadata().getOrDefault("denied", false);
                if (!Boolean.TRUE.equals(denied)) {
                    String reqStr = (String) event.metadata().getOrDefault("arguments", "");
                    this.toolExecutionStore.saveStarted(event.runId(), event.threadId(), toolName, desc, reqStr, event.metadata());
                }
            } else if (event.type() == AgentEventType.TOOL_COMPLETED || event.type() == AgentEventType.TOOL_DENIED) {
                String toolName = (String) event.metadata().get("toolName");
                if (toolName == null) toolName = (String) event.metadata().get("tool");
                Boolean denied = (Boolean) event.metadata().getOrDefault("denied", false);
                if (Boolean.TRUE.equals(denied)) {
                    this.toolExecutionStore.saveDenied(event.runId(), event.threadId(), toolName,
                            (String) event.metadata().getOrDefault("reason", "Tool denied"),
                            event.metadata());
                } else {
                    long duration = ((Number) event.metadata().getOrDefault("durationMs", 0L)).longValue();
                    String status = (String) event.metadata().getOrDefault("status", "COMPLETED");
                    if ("FAILED".equals(status) || "NOT_FOUND".equals(status)
                            || (event.content() != null && event.content().startsWith("Tool failed:"))) {
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


