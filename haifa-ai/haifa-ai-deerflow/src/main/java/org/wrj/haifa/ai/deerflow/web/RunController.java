package org.wrj.haifa.ai.deerflow.web;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchQualityGate;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRuntime;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentEventEntity;
import org.wrj.haifa.ai.deerflow.persistence.entity.ModelStepEntity;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolCallEntity;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolExecutionStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStatus;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.approval.ApprovalRequestRecord;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStatus;
import java.util.Optional;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.todo.TodoSnapshot;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/deerflow/runs")
public class RunController {

    private final AgentRuntime agentRuntime;
    private final RunManager runManager;
    private final AgentEventStore agentEventStore;
    private final ToolExecutionStore toolExecutionStore;
    private final ToolCallStore toolCallStore;
    private final ModelStepStore modelStepStore;
    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final ResearchPlanStore researchPlanStore;
    private final ResearchProgressTracker researchProgressTracker;
    private final ResearchQualityGate researchQualityGate;
    private final ClarificationStore clarificationStore;
    private final MessageStore messageStore;
    private final ApprovalStore approvalStore;
    private final TodoStore todoStore;
    private static final java.time.format.DateTimeFormatter ISO = java.time.format.DateTimeFormatter.ISO_INSTANT;

    public RunController(AgentRuntime agentRuntime, RunManager runManager,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ToolCallStore toolCallStore, ModelStepStore modelStepStore,
            ResearchRuntimeSupport researchRuntimeSupport,
            ResearchPlanStore researchPlanStore,
            ResearchProgressTracker researchProgressTracker,
            ResearchQualityGate researchQualityGate,
            ClarificationStore clarificationStore,
            MessageStore messageStore) {
        this(agentRuntime, runManager, agentEventStore, toolExecutionStore, toolCallStore, modelStepStore,
             researchRuntimeSupport, researchPlanStore, researchProgressTracker, researchQualityGate,
             clarificationStore, messageStore, null, null);
    }

    @Autowired
    public RunController(AgentRuntime agentRuntime, RunManager runManager,
            AgentEventStore agentEventStore, ToolExecutionStore toolExecutionStore,
            ToolCallStore toolCallStore, ModelStepStore modelStepStore,
            ResearchRuntimeSupport researchRuntimeSupport,
            ResearchPlanStore researchPlanStore,
            ResearchProgressTracker researchProgressTracker,
            ResearchQualityGate researchQualityGate,
            ClarificationStore clarificationStore,
            MessageStore messageStore,
            @Autowired(required = false) ApprovalStore approvalStore,
            @Autowired(required = false) TodoStore todoStore) {
        this.agentRuntime = agentRuntime;
        this.runManager = runManager;
        this.agentEventStore = agentEventStore;
        this.toolExecutionStore = toolExecutionStore;
        this.toolCallStore = toolCallStore;
        this.modelStepStore = modelStepStore;
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.researchPlanStore = researchPlanStore;
        this.researchProgressTracker = researchProgressTracker;
        this.researchQualityGate = researchQualityGate;
        this.clarificationStore = clarificationStore;
        this.messageStore = messageStore;
        this.approvalStore = approvalStore;
        this.todoStore = todoStore;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.http.ResponseEntity<Flux<ServerSentEvent<AgentEvent>>> stream(
            @Valid @RequestBody RunCreateRequest request,
            org.springframework.web.server.ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);

        if (request.threadId() != null && !request.threadId().isBlank() && this.clarificationStore != null) {
            Optional<ClarificationRecord> pendingClarification = this.clarificationStore.findPending(request.threadId());
            if (pendingClarification.isPresent()) {
                if (request.message() == null || request.message().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clarification answer is required");
                }
                ClarificationRecord answered = this.clarificationStore.answer(
                        pendingClarification.get().clarificationId(),
                        request.message());
                return resumeAnsweredRun(answered.runId(), userId);
            }
        }

        Flux<ServerSentEvent<AgentEvent>> body = this.agentRuntime.stream(new AgentRequest(
                request.threadId(), request.message(), request.model(), request.uploadedFileIds(),
                request.mode(), request.researchOptions(), userId))
                .map(event -> ServerSentEvent.<AgentEvent>builder(event)
                        .id(event.runId() + ":" + event.eventId())
                        .event(event.type().name().toLowerCase())
                        .build());
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    @PostMapping(path = "/{runId}/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.http.ResponseEntity<Flux<ServerSentEvent<AgentEvent>>> resume(
            @PathVariable String runId,
            org.springframework.web.server.ServerWebExchange exchange) {
        String userId = UserIdResolver.resolve(exchange);
        return resumeAnsweredRun(runId, userId);
    }

    private org.springframework.http.ResponseEntity<Flux<ServerSentEvent<AgentEvent>>> resumeAnsweredRun(
            String runId,
            String userId) {
        org.wrj.haifa.ai.deerflow.run.RunRecord originalRun = this.runManager.find(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));

        Optional<ClarificationRecord> clarificationOpt = this.clarificationStore.findByRunId(runId);
        Optional<ApprovalRequestRecord> approvalOpt = this.approvalStore != null
                ? this.approvalStore.findByRunId(runId).stream()
                        .filter(a -> a.status() == ApprovalStatus.APPROVED || a.status() == ApprovalStatus.DENIED || a.status() == ApprovalStatus.EXPIRED)
                        .findFirst()
                : Optional.empty();

        if (clarificationOpt.isEmpty() && approvalOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No clarification or approval record found for this run");
        }

        List<org.wrj.haifa.ai.deerflow.thread.MessageRecord> runMessages = this.messageStore.listByRun(runId);
        String originalMessage = runMessages.stream()
                .filter(m -> m.role() == org.wrj.haifa.ai.deerflow.thread.MessageRole.USER)
                .map(org.wrj.haifa.ai.deerflow.thread.MessageRecord::content)
                .findFirst()
                .orElse("");

        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("resumedFromRunId", runId);

        if (clarificationOpt.isPresent()) {
            ClarificationRecord clarification = clarificationOpt.get();
            if (clarification.status() != ClarificationStatus.ANSWERED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clarification has not been answered yet");
            }
            metadata.put("clarificationId", clarification.clarificationId());
        } else {
            ApprovalRequestRecord approval = approvalOpt.get();
            metadata.put("approvalId", approval.approvalId());
            metadata.put("approvalDecision", approval.decisionType() != null ? approval.decisionType().name() : approval.status().name());
        }

        org.wrj.haifa.ai.deerflow.agent.ResearchOptions options = originalRun.metadata() != null ?
                reconstructResearchOptions(originalRun.metadata()) :
                org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults();

        org.wrj.haifa.ai.deerflow.agent.RunMode runMode = "research".equalsIgnoreCase(originalRun.mode()) ?
                org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH : org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT;

        AgentRequest resumeRequest = new AgentRequest(
                originalRun.threadId(),
                originalMessage,
                originalRun.modelName(),
                List.of(),
                runMode,
                options,
                userId,
                metadata
        );

        Flux<ServerSentEvent<AgentEvent>> body = this.agentRuntime.stream(resumeRequest)
                .map(event -> ServerSentEvent.<AgentEvent>builder(event)
                        .id(event.runId() + ":" + event.eventId())
                        .event(event.type().name().toLowerCase())
                        .build());

        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    private org.wrj.haifa.ai.deerflow.agent.ResearchOptions reconstructResearchOptions(java.util.Map<String, Object> metadata) {
        if (metadata == null) {
            return org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults();
        }
        try {
            org.wrj.haifa.ai.deerflow.agent.ResearchDepth depth = org.wrj.haifa.ai.deerflow.agent.ResearchDepth.valueOf(
                    String.valueOf(metadata.getOrDefault("depth", "STANDARD"))
            );
            org.wrj.haifa.ai.deerflow.agent.ResearchTimeWindow timeWindow = org.wrj.haifa.ai.deerflow.agent.ResearchTimeWindow.valueOf(
                    String.valueOf(metadata.getOrDefault("timeWindow", "LATEST"))
            );
            int maxSources = Integer.parseInt(String.valueOf(metadata.getOrDefault("maxSources", "10")));
            boolean requireCitations = Boolean.parseBoolean(String.valueOf(metadata.getOrDefault("requireCitations", "true")));
            org.wrj.haifa.ai.deerflow.agent.ResearchOutputFormat outputFormat = org.wrj.haifa.ai.deerflow.agent.ResearchOutputFormat.valueOf(
                    String.valueOf(metadata.getOrDefault("outputFormat", "ANSWER"))
            );
            return new org.wrj.haifa.ai.deerflow.agent.ResearchOptions(depth, timeWindow, maxSources, requireCitations, outputFormat);
        } catch (Exception e) {
            return org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults();
        }
    }


    @GetMapping("/{runId}")
    public Mono<RunResponse> get(@PathVariable String runId) {
        return Mono.justOrEmpty(this.runManager.find(runId))
                .map(RunController::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found")));
    }

    @GetMapping("/{runId}/events")
    public Mono<List<AgentEvent>> events(@PathVariable String runId) {
        return Mono.just(this.agentEventStore.findByRunId(runId));
    }

    @GetMapping("/{runId}/todos")
    public Mono<TodoSnapshot> todos(@PathVariable String runId) {
        RunRecord run = this.runManager.find(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        if (this.todoStore == null) {
            return Mono.just(TodoSnapshot.of(run.threadId(), run.runId(), 0, "read", List.of()));
        }
        return Mono.just(this.todoStore.snapshot(run.threadId(), run.runId()));
    }

    @GetMapping("/{runId}/approvals")
    public Mono<List<ApprovalRequestRecord>> approvals(@PathVariable String runId) {
        if (this.approvalStore == null) {
            return Mono.just(List.of());
        }
        return Mono.just(this.approvalStore.findByRunId(runId));
    }

    @GetMapping("/{runId}/tool-executions")
    public Mono<List<ToolExecutionEntity>> toolExecutions(@PathVariable String runId) {
        return Mono.just(this.toolExecutionStore.findByRunId(runId));
    }

    @GetMapping("/{runId}/tool-calls")
    public Mono<List<ToolCallEntity>> toolCalls(@PathVariable String runId) {
        return Mono.just(this.toolCallStore.findByRunId(runId));
    }

    @GetMapping("/{runId}/model-steps")
    public Mono<List<ModelStepEntity>> modelSteps(@PathVariable String runId) {
        return Mono.just(this.modelStepStore.findByRunId(runId));
    }

    @GetMapping("/{runId}/sources")
    public Mono<List<ResearchSourceResponse>> sources(@PathVariable String runId) {
        return Mono.just(this.researchRuntimeSupport.listSourcesByRun(runId).stream()
                .map(source -> toResearchSourceResponse(runId, source))
                .toList());
    }

    @GetMapping("/{runId}/evidence")
    public Mono<List<EvidenceItemResponse>> evidence(@PathVariable String runId) {
        return Mono.just(this.researchRuntimeSupport.listEvidenceByRun(runId).stream()
                .map(this::toEvidenceItemResponse)
                .toList());
    }

    private static RunResponse toResponse(RunRecord record) {
        return new RunResponse(record.runId(), record.threadId(), record.modelName(), record.status(), record.error(),
                record.mode(), record.createdAt(), record.updatedAt());
    }

    private ResearchSourceResponse toResearchSourceResponse(String runId, ResearchSource source) {
        return new ResearchSourceResponse(
                source.sourceId(),
                source.title(),
                source.url(),
                source.domain(),
                source.publishedAt(),
                source.fetchedAt(),
                source.sourceType().name(),
                source.credibility(),
                source.snippet(),
                source.contentHash(),
                source.fetched(),
                this.researchRuntimeSupport.citationCount(runId, source.sourceId())
        );
    }

    private EvidenceItemResponse toEvidenceItemResponse(EvidenceItem evidenceItem) {
        ResearchSource source = this.researchRuntimeSupport.listSourcesByRun(evidenceItem.runId()).stream()
                .filter(candidate -> candidate.sourceId().equals(evidenceItem.sourceId()))
                .findFirst()
                .orElse(null);
        return new EvidenceItemResponse(
                evidenceItem.evidenceId(),
                evidenceItem.sourceId(),
                source == null ? "" : source.title(),
                source == null ? "" : source.url(),
                evidenceItem.quoteOrParaphrase(),
                evidenceItem.claim(),
                evidenceItem.dimension(),
                evidenceItem.confidence(),
                evidenceItem.extractedAt()
        );
    }

    @GetMapping("/{runId}/plan")
    public Mono<ResearchPlanResponse> plan(@PathVariable String runId) {
        return Mono.justOrEmpty(this.researchPlanStore.findByRunId(runId))
                .map(this::toPlanResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found")));
    }

    @GetMapping("/{runId}/progress")
    public Mono<ResearchProgressResponse> progress(@PathVariable String runId) {
        requireResearchPlan(runId);
        ResearchProgressTracker.ResearchProgress progress = this.researchProgressTracker.getProgress(runId);
        return Mono.just(new ResearchProgressResponse(
                progress.totalDimensions(),
                progress.completedDimensions(),
                progress.inProgressDimensions(),
                progress.totalSources(),
                progress.totalEvidence(),
                progress.planStatus(),
                progress.completionPercentage(),
                List.of()
        ));
    }

    @GetMapping("/{runId}/quality-gate")
    public Mono<QualityGateResponse> qualityGate(@PathVariable String runId) {
        ResearchPlan plan = requireResearchPlan(runId);
        List<ResearchSource> sources = this.researchRuntimeSupport.listSourcesByRun(runId);
        List<EvidenceItem> evidenceItems = this.researchRuntimeSupport.listEvidenceByRun(runId);
        boolean requireCitations = this.runManager.find(runId)
                .map(RunRecord::metadata)
                .map(metadata -> metadata.get("requireCitations"))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(plan != null);
        QualityGateResult result = this.researchQualityGate.evaluate(plan, sources, evidenceItems, requireCitations);
        return Mono.just(new QualityGateResponse(
                result.passed(),
                result.score(),
                result.gaps(),
                result.recommendation(),
                result.dimensionCount(),
                result.fetchedSourceCount(),
                result.hasFacts(),
                result.hasData(),
                result.hasCases(),
                result.hasOpinions(),
                result.hasLimitations(),
                result.hasCounterView(),
                result.citationComplete()
        ));
    }

    private ResearchPlan requireResearchPlan(String runId) {
        RunRecord run = this.runManager.find(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        if (!"research".equalsIgnoreCase(run.mode())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Research artifacts not found");
        }
        return this.researchPlanStore.findByRunId(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }

    private ResearchPlanResponse toPlanResponse(ResearchPlan plan) {
        return new ResearchPlanResponse(
                plan.planId(),
                plan.threadId(),
                plan.runId(),
                plan.topic(),
                plan.researchQuestions(),
                plan.dimensions().stream().map(this::toDimensionResponse).toList(),
                plan.searchQueries(),
                plan.sourceCriteria(),
                plan.expectedDeliverable(),
                plan.status(),
                plan.createdAt() == null ? null : ISO.format(plan.createdAt()),
                plan.updatedAt() == null ? null : ISO.format(plan.updatedAt())
        );
    }

    private ResearchDimensionResponse toDimensionResponse(ResearchDimension dim) {
        return new ResearchDimensionResponse(
                dim.id(),
                dim.title(),
                dim.description(),
                dim.status().name(),
                dim.searchQueries(),
                dim.expectedSourceCount(),
                dim.actualSourceCount(),
                dim.actualEvidenceCount(),
                dim.evidenceIds()
        );
    }
}
