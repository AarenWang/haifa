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
import org.wrj.haifa.ai.deerflow.artifact.ReportWriteResult;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriterService;
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
import org.wrj.haifa.ai.deerflow.research.plan.ClarificationGate;
import org.wrj.haifa.ai.deerflow.research.plan.PlanGenerationResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchClarificationStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanner;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchQualityGate;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.ResearchLoopObserver;
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
    private final ResearchPlanner researchPlanner;
    private final ResearchPlanStore researchPlanStore;
    private final ClarificationGate clarificationGate;
    private final ResearchClarificationStore researchClarificationStore;
    private final ResearchProgressTracker researchProgressTracker;
    private final ResearchQualityGate researchQualityGate;
    private final ReportWriterService reportWriterService;

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
                agentEventStore, toolExecutionStore, modelStepStore, toolCallStore, agentLoopRunStore, skillStorage,
                null, null, null, null, null, null, null, null, null);
    }

    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage,
            ResearchRuntimeSupport researchRuntimeSupport,
            ResearchPlanner researchPlanner, ResearchPlanStore researchPlanStore,
            ClarificationGate clarificationGate, ResearchClarificationStore researchClarificationStore,
            ResearchProgressTracker researchProgressTracker, ResearchQualityGate researchQualityGate,
            ReportWriterService reportWriterService) {
        this(properties, toolRegistry, modelClient, runManager, threadManager, messageStore, middlewares,
                agentEventStore, toolExecutionStore, modelStepStore, toolCallStore, agentLoopRunStore, skillStorage,
                null, researchRuntimeSupport, researchPlanner, researchPlanStore, clarificationGate,
                researchClarificationStore, researchProgressTracker, researchQualityGate, reportWriterService);
    }

    @Autowired
    public SimpleAgentRuntime(DeerFlowProperties properties, ToolRegistry toolRegistry, AgentModelClient modelClient,
            RunManager runManager, ThreadManager threadManager, MessageStore messageStore, List<AgentMiddleware> middlewares,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage,
            @Autowired(required = false) org.wrj.haifa.ai.deerflow.todo.TodoStore todoStore,
            @Autowired(required = false) ResearchRuntimeSupport researchRuntimeSupport,
            @Autowired(required = false) ResearchPlanner researchPlanner,
            @Autowired(required = false) ResearchPlanStore researchPlanStore,
            @Autowired(required = false) ClarificationGate clarificationGate,
            @Autowired(required = false) ResearchClarificationStore researchClarificationStore,
            @Autowired(required = false) ResearchProgressTracker researchProgressTracker,
            @Autowired(required = false) ResearchQualityGate researchQualityGate,
            @Autowired(required = false) ReportWriterService reportWriterService) {
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
                new ResearchLoopObserver(todoStore, researchRuntimeSupport, researchPlanner, researchPlanStore, researchProgressTracker, researchQualityGate));
        this.agentEventStore = agentEventStore;
        this.toolExecutionStore = toolExecutionStore;
        this.modelStepStore = modelStepStore;
        this.toolCallStore = toolCallStore;
        this.agentLoopRunStore = agentLoopRunStore;
        this.skillStorage = skillStorage;
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.researchPlanner = researchPlanner;
        this.researchPlanStore = researchPlanStore;
        this.clarificationGate = clarificationGate;
        this.researchClarificationStore = researchClarificationStore;
        this.researchProgressTracker = researchProgressTracker;
        this.researchQualityGate = researchQualityGate;
        this.reportWriterService = reportWriterService;
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
            ResearchClarificationStore.PendingClarification pendingClarification = this.researchClarificationStore == null
                    ? null
                    : this.researchClarificationStore.find(requestedThreadId).orElse(null);
            ThreadRecord thread = this.threadManager.upsert(requestedThreadId,
                    ThreadManager.titleFromMessage(request.message()), Map.of("source", "run"));
            String threadId = thread.threadId();
            String modelName = StringUtils.hasText(request.model()) ? request.model() : this.properties.getModel();
            int uploadedFileCount = request.uploadedFileIds() == null ? 0 : request.uploadedFileIds().size();
            ResearchOptions researchOptions = request.researchOptions() == null
                    ? (pendingClarification == null ? ResearchOptions.defaults() : pendingClarification.researchOptions())
                    : request.researchOptions();
            String effectiveResearchMessage = pendingClarification == null
                    ? request.message()
                    : pendingClarification.originalMessage() + System.lineSeparator()
                            + "Clarification from user: " + request.message();
            java.util.Map<String, Object> runMetadataMap = new java.util.LinkedHashMap<>();
            runMetadataMap.put("source", "sse");
            runMetadataMap.put("uploadedFiles", uploadedFileCount);
            runMetadataMap.put("mode", "research");
            runMetadataMap.put("depth", researchOptions.depth().name());
            runMetadataMap.put("timeWindow", researchOptions.timeWindow().name());
            runMetadataMap.put("maxSources", researchOptions.maxSources());
            runMetadataMap.put("requireCitations", researchOptions.requireCitations());
            runMetadataMap.put("outputFormat", researchOptions.outputFormat().name());
            if (pendingClarification != null) {
                runMetadataMap.put("continuationOfRunId", pendingClarification.runId());
                runMetadataMap.put("clarificationType", pendingClarification.clarificationType());
            }
            Map<String, Object> runMetadata = Map.copyOf(runMetadataMap);
            RunRecord run = this.runManager.create(threadId, modelName, runMetadata);
            this.runManager.markRunning(run.runId());
            this.messageStore.add(threadId, run.runId(), MessageRole.USER, request.message(),
                    Map.of("uploadedFileCount", uploadedFileCount, "mode", "research"));
            if (pendingClarification != null && this.researchClarificationStore != null) {
                this.researchClarificationStore.clear(threadId);
            }
            log.info("Research run started. runId={}, threadId={}, model={}, depth={}, timeWindow={}, maxSources={}, outputFormat={}",
                    run.runId(), threadId, nullToEmpty(modelName), researchOptions.depth(), researchOptions.timeWindow(),
                    researchOptions.maxSources(), researchOptions.outputFormat());

            AgentRunConfig config = new AgentRunConfig(threadId, run.runId(), modelName, true, false,
                    this.properties.getMaxIterations(), Path.of(this.properties.getWorkspaceRoot()), RunMode.RESEARCH,
                    researchOptions, Map.of("runtime", "agent-loop"));
            AtomicInteger seq = new AtomicInteger();

            List<AgentEvent> prefixEvents = new ArrayList<>();
            prefixEvents.add(event(seq, config, AgentEventType.RUN_STARTED, "Research run started", runMetadata));

            // --- Clarification Gate ---
            if (this.clarificationGate != null && pendingClarification == null) {
                var clarificationResult = this.clarificationGate.check(effectiveResearchMessage, researchOptions);
                if (clarificationResult.needsClarification()) {
                    if (this.researchClarificationStore != null) {
                        this.researchClarificationStore.save(
                                threadId,
                                run.runId(),
                                effectiveResearchMessage,
                                researchOptions,
                                clarificationResult.clarificationQuestion(),
                                clarificationResult.clarificationType()
                        );
                    }
                    prefixEvents.add(event(seq, config, AgentEventType.TOOL_CALL_REQUESTED,
                            "Clarification needed: " + clarificationResult.clarificationQuestion(),
                            Map.of("clarificationType", clarificationResult.clarificationType(),
                                    "question", clarificationResult.clarificationQuestion(),
                                    "resumeThreadId", threadId,
                                    "resumeRunId", run.runId())));
                    this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM,
                            "Clarification needed: " + clarificationResult.clarificationQuestion(),
                            Map.of("clarificationType", clarificationResult.clarificationType(),
                                    "question", clarificationResult.clarificationQuestion(),
                                    "clarificationPending", true,
                                    "resumeThreadId", threadId,
                                    "resumeRunId", run.runId()));
                    return Flux.fromIterable(prefixEvents)
                            .doOnNext(this::saveEvent)
                            .doOnComplete(() -> {
                                this.runManager.markCompleted(run.runId());
                                this.threadManager.touch(threadId);
                            });
                }
            }

            // --- Research Plan Generation ---
            ResearchPlan plan = null;
            if (this.researchPlanner != null) {
                PlanGenerationResult planResult = this.researchPlanner.generatePlan(threadId, run.runId(), effectiveResearchMessage, researchOptions);
                if (planResult.success() && planResult.plan() != null) {
                    plan = planResult.plan();
                    if (this.researchPlanStore != null) {
                        this.researchPlanStore.save(plan);
                    }
                    if (this.researchProgressTracker != null && !plan.dimensions().isEmpty()) {
                        this.researchProgressTracker.markDimensionStarted(run.runId(), plan.dimensions().get(0).id());
                        plan = this.researchPlanStore == null ? plan : this.researchPlanStore.findByRunId(run.runId()).orElse(plan);
                    }
                    prefixEvents.add(event(seq, config, AgentEventType.RESEARCH_PLAN_CREATED,
                            "Research plan created with " + plan.dimensionCount() + " dimensions",
                            Map.of("planId", plan.planId(), "dimensionCount", plan.dimensionCount(),
                                    "topic", plan.topic(), "depth", researchOptions.depth().name(),
                                    "timeWindow", researchOptions.timeWindow().name(),
                                    "maxSources", researchOptions.maxSources(),
                                    "outputFormat", researchOptions.outputFormat().name())));
                    // Emit dimension started events for the active dimension
                    for (org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension dim : plan.dimensions()) {
                        if (dim.status() == org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus.IN_PROGRESS) {
                            prefixEvents.add(event(seq, config, AgentEventType.RESEARCH_DIMENSION_STARTED,
                                    "Dimension started: " + dim.title(),
                                    Map.of("dimensionId", dim.id(), "dimensionTitle", dim.title(),
                                            "expectedSourceCount", dim.expectedSourceCount())));
                            break;
                        }
                    }
                } else if (planResult.needsClarification()) {
                    if (this.researchClarificationStore != null) {
                        this.researchClarificationStore.save(
                                threadId,
                                run.runId(),
                                effectiveResearchMessage,
                                researchOptions,
                                planResult.clarificationQuestion(),
                                planResult.clarificationType()
                        );
                    }
                    prefixEvents.add(event(seq, config, AgentEventType.TOOL_CALL_REQUESTED,
                            "Plan generation needs clarification: " + planResult.clarificationQuestion(),
                            Map.of("clarificationType", planResult.clarificationType(),
                                    "question", planResult.clarificationQuestion(),
                                    "resumeThreadId", threadId,
                                    "resumeRunId", run.runId())));
                    this.messageStore.add(threadId, run.runId(), MessageRole.SYSTEM,
                            "Plan generation needs clarification: " + planResult.clarificationQuestion(),
                            Map.of("clarificationType", planResult.clarificationType(),
                                    "question", planResult.clarificationQuestion(),
                                    "clarificationPending", true,
                                    "resumeThreadId", threadId,
                                    "resumeRunId", run.runId()));
                    return Flux.fromIterable(prefixEvents)
                            .doOnNext(this::saveEvent)
                            .doOnComplete(() -> {
                                this.runManager.markCompleted(run.runId());
                                this.threadManager.touch(threadId);
                            });
                }
            }
            if (plan == null) {
                prefixEvents.add(event(seq, config, AgentEventType.RESEARCH_PLAN_CREATED,
                        "Research plan created for: " + effectiveResearchMessage,
                        Map.of("depth", researchOptions.depth().name(), "timeWindow", researchOptions.timeWindow().name(),
                                "maxSources", researchOptions.maxSources(), "outputFormat", researchOptions.outputFormat().name())));
            }

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
            Flux<AgentEvent> loopEvents = this.agentLoop.run(loopConfig, config, researchSystemPrompt, effectiveResearchMessage, seq,
                    this.toolPolicyService, activeSkills, request.uploadedFileIds());

            // After loop completes, mark run status based on last event type
            final java.util.concurrent.atomic.AtomicReference<String> lastEventType = new java.util.concurrent.atomic.AtomicReference<>();
            final java.util.concurrent.atomic.AtomicReference<String> lastModelContent = new java.util.concurrent.atomic.AtomicReference<>("");
            final java.util.concurrent.atomic.AtomicReference<Map<String, Object>> lastModelMetadata = new java.util.concurrent.atomic.AtomicReference<>(Map.of());
            final java.util.concurrent.atomic.AtomicReference<org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan> finalPlan = new java.util.concurrent.atomic.AtomicReference<>(plan);
            Flux<AgentEvent> wrappedLoopEvents = loopEvents
                    .map(evt -> {
                        lastEventType.set(evt.type().name());
                        if (this.researchPlanStore != null) {
                            this.researchPlanStore.findByRunId(run.runId()).ifPresent(finalPlan::set);
                        }
                        if (evt.type() == AgentEventType.MODEL_COMPLETED) {
                            lastModelContent.set(evt.content() == null ? "" : evt.content());
                            lastModelMetadata.set(evt.metadata() == null ? Map.of() : evt.metadata());
                            return event(seq, config, AgentEventType.MODEL_COMPLETED,
                                    "Research synthesis captured. Preparing report artifact.",
                                    Map.of("mode", "research", "reportPending", true,
                                            "stopReason", evt.metadata().getOrDefault("stopReason", "")));
                        }
                        return evt;
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
            Flux<AgentEvent> deliveryEvents = Flux.defer(() -> Flux.fromIterable(finishResearchDelivery(
                    lastEventType.get(),
                    run,
                    threadId,
                    config,
                    seq,
                    runStartTime,
                    finalPlan.get(),
                    researchOptions,
                    lastModelContent.get(),
                    lastModelMetadata.get()
            )));

            return Flux.concat(Flux.fromIterable(prefixEvents), wrappedLoopEvents, deliveryEvents)
                    .doOnNext(this::saveEvent);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<AgentEvent> finishResearchDelivery(String lastType, RunRecord run, String threadId,
            AgentRunConfig config, AtomicInteger seq, long runStartTime, ResearchPlan plan,
            ResearchOptions researchOptions, String synthesisResult, Map<String, Object> modelMetadata) {
        long totalDuration = System.currentTimeMillis() - runStartTime;
        if ("RUN_FAILED".equals(lastType)) {
            this.runManager.markFailed(run.runId(), "Research run failed");
            this.threadManager.touch(threadId);
            log.info("Research run failed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
            return List.of();
        }
        if ("RUN_CANCELLED".equals(lastType)) {
            this.threadManager.touch(threadId);
            log.info("Research run cancelled. runId={}, totalDurationMs={}", run.runId(), totalDuration);
            return List.of();
        }

        List<AgentEvent> events = new ArrayList<>();
        List<ResearchSource> sources = this.researchRuntimeSupport == null
                ? List.of()
                : this.researchRuntimeSupport.listSourcesByRun(run.runId());
        List<EvidenceItem> evidenceItems = this.researchRuntimeSupport == null
                ? List.of()
                : this.researchRuntimeSupport.listEvidenceByRun(run.runId());
        QualityGateResult qgResult = null;

        if (this.researchQualityGate != null) {
            events.add(event(seq, config, AgentEventType.QUALITY_GATE_STARTED,
                    "Quality gate evaluation started", Map.of()));
            qgResult = this.researchQualityGate.evaluate(plan, sources, evidenceItems,
                    researchOptions == null || Boolean.TRUE.equals(researchOptions.requireCitations()));
            if (qgResult.passed()) {
                events.add(event(seq, config, AgentEventType.QUALITY_GATE_PASSED,
                        "Quality gate passed. Score: " + String.format("%.1f", qgResult.score()),
                        Map.of("score", qgResult.score(), "dimensionCount", qgResult.dimensionCount(),
                                "fetchedSourceCount", qgResult.fetchedSourceCount())));
            } else {
                events.add(event(seq, config, AgentEventType.QUALITY_GATE_FAILED,
                        "Quality gate failed. Score: " + String.format("%.1f", qgResult.score())
                                + ". Gaps: " + String.join("; ", qgResult.gaps()),
                        Map.of("score", qgResult.score(), "gaps", qgResult.gaps(),
                                "recommendation", qgResult.recommendation(),
                                "dimensionCount", qgResult.dimensionCount(),
                                "fetchedSourceCount", qgResult.fetchedSourceCount())));
            }
        }

        if (this.reportWriterService != null) {
            events.add(event(seq, config, AgentEventType.REPORT_STARTED,
                    "Research report generation started", Map.of()));
            ReportWriteResult result = this.reportWriterService.writeReport(
                    threadId, run.runId(), plan, sources, evidenceItems, qgResult,
                    synthesisResult, researchOptions);
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
                    buildArtifactSummary(result),
                    Map.of("mode", "research", "artifactId", result.artifact().artifactId(),
                            "filename", result.artifact().filename(),
                            "toolCount", modelMetadata == null ? 0 : modelMetadata.getOrDefault("totalToolCalls", 0)));
        } else {
            this.messageStore.add(threadId, run.runId(), MessageRole.ASSISTANT,
                    compactResearchAnswer(synthesisResult, qgResult),
                    Map.of("mode", "research", "artifactUnavailable", true));
        }

        this.runManager.markCompleted(run.runId());
        this.threadManager.touch(threadId);
        log.info("Research run completed. runId={}, totalDurationMs={}", run.runId(), totalDuration);
        return events;
    }

    private static String buildArtifactSummary(ReportWriteResult result) {
        return "Summary\n\n" + result.summary()
                + "\n\nLimitations\n\n" + result.limitations()
                + "\n\nArtifact\n\n- [" + result.artifact().filename() + "](/api/deerflow/artifacts/"
                + result.artifact().artifactId() + "/download)";
    }

    private static String compactResearchAnswer(String synthesisResult, QualityGateResult qualityGateResult) {
        String answer = synthesisResult == null ? "" : synthesisResult
                .replace("<final_answer>", "")
                .replace("</final_answer>", "")
                .trim();
        if (answer.length() > 900) {
            answer = answer.substring(0, 900).trim() + "...";
        }
        String limitations = qualityGateResult == null || qualityGateResult.passed()
                ? "No blocking quality gate gaps were recorded."
                : String.join("; ", qualityGateResult.gaps());
        return "Summary\n\n" + (answer.isBlank() ? "Research completed." : answer)
                + "\n\nLimitations\n\n" + limitations;
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
