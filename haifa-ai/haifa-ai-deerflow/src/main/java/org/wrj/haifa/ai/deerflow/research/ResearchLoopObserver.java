package org.wrj.haifa.ai.deerflow.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.DefaultAgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.FinalAnswerDecision;
import org.wrj.haifa.ai.deerflow.agent.loop.FinalAnswerResult;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanner;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchQualityGate;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wrj.haifa.ai.deerflow.runstate.SkillActivationStore;
import org.wrj.haifa.ai.deerflow.work.WorkItemStore;
import org.wrj.haifa.ai.deerflow.source.Source;
import org.wrj.haifa.ai.deerflow.source.SourceStore;
import org.wrj.haifa.ai.deerflow.evidence.EvidenceItemStore;
import org.wrj.haifa.ai.deerflow.claim.ClaimStore;
import org.wrj.haifa.ai.deerflow.claim.CitationStore;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedgerStore;
import org.wrj.haifa.ai.deerflow.quality.QualityAssessmentStore;
import org.wrj.haifa.ai.deerflow.threadfile.ThreadFileStore;

/**
 * AgentLoopObserver implementation for research-specific logic.
 * Tool output compression is handled centrally by {@link org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop}.
 */
public class ResearchLoopObserver extends DefaultAgentLoopObserver {

    private static final Logger log = LoggerFactory.getLogger(ResearchLoopObserver.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final ResearchPlanner researchPlanner;
    private final ResearchPlanStore researchPlanStore;
    private final ResearchProgressTracker researchProgressTracker;
    private final ResearchQualityGate researchQualityGate;

    private SkillActivationStore skillActivationStore;
    private WorkItemStore workItemStore;
    private SourceStore sourceStore;
    private EvidenceItemStore newEvidenceItemStore;
    private ClaimStore claimStore;
    private CitationStore citationStore;
    private BudgetLedgerStore budgetLedgerStore;
    private QualityAssessmentStore qualityAssessmentStore;
    private ThreadFileStore threadFileStore;

    public ResearchLoopObserver(ResearchRuntimeSupport researchRuntimeSupport,
            ResearchPlanner researchPlanner, ResearchPlanStore researchPlanStore,
            ResearchProgressTracker researchProgressTracker, ResearchQualityGate researchQualityGate) {
        this(null, researchRuntimeSupport, researchPlanner, researchPlanStore, researchProgressTracker, researchQualityGate);
    }

    public ResearchLoopObserver(TodoStore todoStore, ResearchRuntimeSupport researchRuntimeSupport,
            ResearchPlanner researchPlanner, ResearchPlanStore researchPlanStore,
            ResearchProgressTracker researchProgressTracker, ResearchQualityGate researchQualityGate) {
        super(todoStore);
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.researchPlanner = researchPlanner;
        this.researchPlanStore = researchPlanStore;
        this.researchProgressTracker = researchProgressTracker;
        this.researchQualityGate = researchQualityGate;
    }

    public ResearchLoopObserver(TodoStore todoStore, ResearchRuntimeSupport researchRuntimeSupport,
            ResearchPlanner researchPlanner, ResearchPlanStore researchPlanStore,
            ResearchProgressTracker researchProgressTracker, ResearchQualityGate researchQualityGate,
            SkillActivationStore skillActivationStore, WorkItemStore workItemStore,
            SourceStore sourceStore, EvidenceItemStore newEvidenceItemStore,
            ClaimStore claimStore, CitationStore citationStore,
            BudgetLedgerStore budgetLedgerStore, QualityAssessmentStore qualityAssessmentStore,
            ThreadFileStore threadFileStore) {
        this(todoStore, researchRuntimeSupport, researchPlanner, researchPlanStore, researchProgressTracker, researchQualityGate);
        this.skillActivationStore = skillActivationStore;
        this.workItemStore = workItemStore;
        this.sourceStore = sourceStore;
        this.newEvidenceItemStore = newEvidenceItemStore;
        this.claimStore = claimStore;
        this.citationStore = citationStore;
        this.budgetLedgerStore = budgetLedgerStore;
        this.qualityAssessmentStore = qualityAssessmentStore;
        this.threadFileStore = threadFileStore;
    }

    private String checkAndIncrementBudget(String runId, String toolName) {
        if (budgetLedgerStore == null) return null;
        return budgetLedgerStore.findByRunId(runId).map(ledger -> {
            budgetLedgerStore.incrementToolCalls(runId);
            int updatedToolCalls = ledger.getUsedToolCalls() + 1;
            if (updatedToolCalls > ledger.getMaxToolCalls()) {
                budgetLedgerStore.updateStopReason(runId, "MAX_TOOL_CALLS_REACHED");
                return "BUDGET_EXCEEDED: max tool calls reached (" + ledger.getMaxToolCalls() + ")";
            }

            if ("web_search".equals(toolName)) {
                budgetLedgerStore.incrementSearchQueries(runId);
                int updatedSearch = ledger.getUsedSearchQueries() + 1;
                if (updatedSearch > ledger.getMaxSearchQueries()) {
                    budgetLedgerStore.updateStopReason(runId, "MAX_SEARCH_QUERIES_REACHED");
                    return "BUDGET_EXCEEDED: max search queries reached (" + ledger.getMaxSearchQueries() + ")";
                }
            } else if ("web_fetch".equals(toolName)) {
                budgetLedgerStore.incrementFetchedSources(runId);
                int updatedFetch = ledger.getUsedFetchedSources() + 1;
                if (updatedFetch > ledger.getMaxFetchedSources()) {
                    budgetLedgerStore.updateStopReason(runId, "MAX_FETCHED_SOURCES_REACHED");
                    return "BUDGET_EXCEEDED: max fetched sources reached (" + ledger.getMaxFetchedSources() + ")";
                }
            }
            return null;
        }).orElse(null);
    }

    private String checkAndIncrementModelCalls(String runId) {
        if (budgetLedgerStore == null) return null;
        return budgetLedgerStore.findByRunId(runId).map(ledger -> {
            budgetLedgerStore.incrementModelCalls(runId);
            int updatedModelCalls = ledger.getUsedModelCalls() + 1;
            if (updatedModelCalls > ledger.getMaxModelCalls()) {
                budgetLedgerStore.updateStopReason(runId, "MAX_MODEL_CALLS_REACHED");
                return "BUDGET_EXCEEDED: max model calls reached (" + ledger.getMaxModelCalls() + ")";
            }
            return null;
        }).orElse(null);
    }

    private boolean isResearchActive(String runId) {
        if (skillActivationStore == null) {
            return this.researchRuntimeSupport != null || this.researchPlanStore != null || this.budgetLedgerStore != null;
        }
        return skillActivationStore.isSkillActive(runId, "deep-research");
    }

    @Override
    public ToolCallResult beforeToolExecute(AgentRunConfig runConfig, ToolCall toolCall) {
        if (!isResearchActive(runConfig.runId())) {
            return super.beforeToolExecute(runConfig, toolCall);
        }

        String budgetStop = checkAndIncrementBudget(runConfig.runId(), toolCall.toolName());
        if (budgetStop != null) {
            return ToolCallResult.fromError(toolCall, budgetStop, 0);
        }

        if (runConfig.mode() != RunMode.RESEARCH) {
            return super.beforeToolExecute(runConfig, toolCall);
        }
        if (this.researchRuntimeSupport == null) {
            return null;
        }
        String toolName = toolCall.toolName();
        if ("web_fetch".equals(toolName)) {
            String url = extractJsonValue(toolCall.arguments(), "url");
            if (!url.isBlank()) {
                Optional<RegisteredSourceContent> cached = this.researchRuntimeSupport.reuseFetched(url);
                if (cached.isPresent()) {
                    RegisteredSourceContent stored = cached.get();
                    return new ToolCallResult(toolCall.id(), toolCall.toolName(), toolCall.arguments(),
                            ToolCallResult.Status.SUCCESS, stored.rawContent(), "", 0,
                            Map.of("url", url, "cached", true, "sourceId", stored.source().sourceId()));
                }
            }
        }
        return null;
    }

    @Override
    public String onToolCompleted(AgentRunConfig runConfig, ToolCall toolCall, ToolCallResult toolResult,
            List<AgentEvent> events, AtomicInteger seq, List<String> history) {
        if (!isResearchActive(runConfig.runId())) {
            return super.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
        }

        if (runConfig.mode() != RunMode.RESEARCH) {
            return super.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
        }
        if (toolResult.status() != ToolCallResult.Status.SUCCESS) {
            return null;
        }

        if ("web_search".equals(toolCall.toolName())) {
            if (this.sourceStore != null) {
                try {
                    JsonNode root = MAPPER.readTree(toolResult.result());
                    if (root.isArray()) {
                        for (JsonNode item : root) {
                            String url = item.has("link") ? item.get("link").asText() : (item.has("url") ? item.get("url").asText() : "");
                            String title = item.has("title") ? item.get("title").asText() : "";
                            String domain = item.has("domain") ? item.get("domain").asText() : "";
                            if (!url.isBlank()) {
                                this.sourceStore.discover(runConfig.runId(), runConfig.threadId(), url, title, domain);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to parse search results for SourceStore discovery: {}", ex.getMessage());
                }
            }

            if (this.researchRuntimeSupport != null) {
                SearchIngestionResult ingestionResult = this.researchRuntimeSupport.ingestSearchResults(
                        runConfig.threadId(), runConfig.runId(), toolResult.result());
                for (var registration : ingestionResult.registrations()) {
                    if (this.sourceStore != null && registration.source() != null) {
                        try {
                            this.sourceStore.discover(
                                    runConfig.runId(),
                                    runConfig.threadId(),
                                    registration.source().url(),
                                    registration.source().title(),
                                    registration.source().domain());
                        } catch (Exception ex) {
                            log.warn("Failed to mirror search result into unified SourceStore: {}", ex.getMessage());
                        }
                    }
                    events.add(event(seq, runConfig, AgentEventType.SOURCE_FOUND,
                            registration.source().title(),
                            Map.of("sourceId", registration.source().sourceId(),
                                    "url", registration.source().url(),
                                    "domain", registration.source().domain(),
                                    "deduplicated", registration.deduplicated())));
                }
                return ingestionResult.observation();
            }
            return null;
        }

        if ("web_fetch".equals(toolCall.toolName())) {
            String url = stringValue(toolResult.metadata().get("url"));
            if (url.isBlank()) {
                url = extractJsonValue(toolCall.arguments(), "url");
            }
            if (url.isBlank()) {
                return null;
            }
            final String finalUrl = url;

            Source unifiedSource = null;
            if (this.sourceStore != null) {
                try {
                    String textContent = toolResult.result();
                    String contentHash = org.springframework.util.DigestUtils.md5DigestAsHex(textContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    String title = stringValue(toolResult.metadata().get("title"));

                    unifiedSource = this.sourceStore.findByUrlAndThreadIdAndRunId(finalUrl, runConfig.threadId(), runConfig.runId())
                            .orElseGet(() -> this.sourceStore.discover(runConfig.runId(), runConfig.threadId(), finalUrl, title.isBlank() ? "Fetched Page" : title, ""));

                    unifiedSource = this.sourceStore.updateFetched(unifiedSource.getSourceId(), finalUrl, title.isBlank() ? unifiedSource.getTitle() : title, "web_page", "high", contentHash, "{}");
                } catch (Exception ex) {
                    log.warn("Failed to update fetched SourceStore: {}", ex.getMessage());
                }
            }

            if (this.researchRuntimeSupport != null) {
                FetchProcessingResult fetchProcessingResult = this.researchRuntimeSupport.ingestFetchedContent(
                        runConfig.threadId(), runConfig.runId(), url, toolResult.result());
                events.add(event(seq, runConfig, AgentEventType.SOURCE_FETCHED,
                        fetchProcessingResult.registration().stored().source().title(),
                        Map.of("sourceId", fetchProcessingResult.registration().stored().source().sourceId(),
                                "url", fetchProcessingResult.registration().stored().source().url(),
                                "domain", fetchProcessingResult.registration().stored().source().domain(),
                                "cached", fetchProcessingResult.registration().cached(),
                                "deduplicatedByUrl", fetchProcessingResult.registration().deduplicatedByUrl(),
                                "deduplicatedByContentHash", fetchProcessingResult.registration().deduplicatedByContentHash())));
                
                if (this.researchProgressTracker != null && !fetchProcessingResult.registration().cached()) {
                    this.researchProgressTracker.recordFetchedSource(
                            runConfig.runId(),
                            fetchProcessingResult.registration().stored().source().sourceId()
                    );
                }

                for (var evidenceItem : fetchProcessingResult.evidenceItems()) {
                    events.add(event(seq, runConfig, AgentEventType.EVIDENCE_EXTRACTED,
                            evidenceItem.claim(),
                            Map.of("evidenceId", evidenceItem.evidenceId(),
                                    "sourceId", evidenceItem.sourceId(),
                                    "dimension", evidenceItem.dimension(),
                                    "confidence", evidenceItem.confidence())));
                    if (this.researchProgressTracker != null) {
                        this.researchProgressTracker.recordEvidence(runConfig.runId(), evidenceItem);
                    }
                    if (this.newEvidenceItemStore != null && unifiedSource != null) {
                        try {
                            String workItemId = firstWorkItemId(runConfig.runId());
                            var created = this.newEvidenceItemStore.create(
                                    unifiedSource.getSourceId(),
                                    runConfig.runId(),
                                    runConfig.threadId(),
                                    workItemId,
                                    evidenceItem.claim(),
                                    evidenceItem.quoteOrParaphrase(),
                                    finalUrl,
                                    evidenceItem.confidence());
                            if (this.workItemStore != null && workItemId != null) {
                                this.workItemStore.addEvidenceId(workItemId, created.getEvidenceId());
                            }
                        } catch (Exception ex) {
                            log.warn("Failed to write unified EvidenceItem: {}", ex.getMessage());
                        }
                    }
                }
                
                return fetchProcessingResult.observation();
            }
            return null;
        }

        return null;
    }

    @Override
    public void onStepCompleted(AgentRunConfig runConfig, List<AgentEvent> events, AtomicInteger seq, int step) {
        if (runConfig.mode() != RunMode.RESEARCH) {
            super.onStepCompleted(runConfig, events, seq, step);
            return;
        }
        if (researchPlanStore != null && researchProgressTracker != null) {
            advanceDimensionIfNeeded(runConfig, events, seq, step);
        }
    }

    @Override
    public boolean shouldContinue(AgentRunConfig runConfig, String responseContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls, List<String> history) {
        if (!isResearchActive(runConfig.runId())) {
            return super.shouldContinue(runConfig, responseContent, events, seq, step, totalToolCalls, history);
        }

        String modelBudgetStop = checkAndIncrementModelCalls(runConfig.runId());
        if (modelBudgetStop != null) {
            events.add(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                    modelBudgetStop,
                    Map.of("stopReason", "BUDGET_EXCEEDED", "step", step, "totalToolCalls", totalToolCalls)));
            return false;
        }

        if (runConfig.mode() != RunMode.RESEARCH) {
            return super.shouldContinue(runConfig, responseContent, events, seq, step, totalToolCalls, history);
        }
        QualityGateResult readiness = evaluateResearchReadiness(runConfig);
        if (shouldContinueResearch(runConfig, readiness)) {
            history.add("System: " + buildContinuationInstruction(runConfig, readiness));
            events.add(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                    "Research requires more coverage before a final answer can be accepted",
                    Map.of("step", step, "totalToolCalls", totalToolCalls,
                            "qualityGaps", readiness == null ? List.of("missing_plan") : readiness.gaps())));
            return true;
        }
        return false;
    }

    @Override
    public FinalAnswerDecision onFinalAnswerProposed(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        FinalAnswerDecision todoDecision = super.onFinalAnswerProposed(runConfig, rawAnswer, events, seq, step, totalToolCalls);
        if (!isResearchActive(runConfig.runId()) || runConfig.mode() != RunMode.RESEARCH) {
            return todoDecision;
        }

        QualityGateResult readiness = evaluateResearchReadiness(runConfig);
        Map<String, Object> qualityMetadata = new java.util.HashMap<>();
        qualityMetadata.put("legacyResearchGate", true);
        qualityMetadata.put("qualityGaps", readiness == null ? List.of("missing_plan") : readiness.gaps());
        qualityMetadata.put("step", step);
        qualityMetadata.put("totalToolCalls", totalToolCalls);
        
        if (this.qualityAssessmentStore != null) {
            try {
                double score = readiness != null ? readiness.score() : 0.0;
                boolean passed = readiness != null && readiness.passed();
                List<String> gaps = readiness != null ? readiness.gaps() : new ArrayList<>(List.of("missing_evaluation"));
                List<String> risks = new ArrayList<>();
                String textAction = passed ? "synthesize" : "continue";
                String limitations = readiness != null ? readiness.recommendation() : "No evaluation performed";
                
                this.qualityAssessmentStore.save(runConfig.runId(), score, passed, gaps, risks, textAction, limitations);
            } catch (Exception ex) {
                log.error("Failed to write QualityAssessment record: {}", ex.getMessage());
            }
        }

        boolean continueResearch = shouldContinueResearch(runConfig, readiness);
        if (!todoDecision.accepted()) {
            Map<String, Object> metadata = new java.util.HashMap<>(qualityMetadata);
            if (todoDecision.metadata() != null) {
                metadata.putAll(todoDecision.metadata());
            }
            if (continueResearch) {
                metadata.put("researchRetryInstruction", buildContinuationInstruction(runConfig, readiness));
            }
            return FinalAnswerDecision.reject(todoDecision.retryInstruction(), metadata);
        }
        if (continueResearch) {
            return FinalAnswerDecision.reject(buildContinuationInstruction(runConfig, readiness), qualityMetadata);
        }
        return todoDecision;
    }

    @Override
    public FinalAnswerResult onFinalAnswerAccepted(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        // Run default checks (e.g. todo items completeness and limitations appending)
        FinalAnswerResult baseResult = super.onFinalAnswerAccepted(runConfig, rawAnswer, events, seq, step, totalToolCalls);

        if (!isResearchActive(runConfig.runId())) {
            return baseResult;
        }

        if (runConfig.mode() != RunMode.RESEARCH) {
            return baseResult;
        }

        CitationProcessingResult citationProcessingResult = finalizeResearchAnswer(runConfig, baseResult.finalAnswer());
        persistUnifiedClaims(runConfig, citationProcessingResult.finalAnswer());
        Map<String, Object> extraMetadata = Map.of(
                "citedSources", citationProcessingResult.citedSources().stream().map(ResearchSource::sourceId).toList(),
                "citedEvidence", citationProcessingResult.citations().stream().flatMap(c -> c.evidenceIds().stream()).distinct().toList()
        );
        return new FinalAnswerResult(citationProcessingResult.finalAnswer(), extraMetadata);
    }

    @Override
    public void onMaxStepsReached(AgentRunConfig runConfig, String lastModelContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        if (!isResearchActive(runConfig.runId())) {
            super.onMaxStepsReached(runConfig, lastModelContent, events, seq, step, totalToolCalls);
            return;
        }

        if (runConfig.mode() != RunMode.RESEARCH) {
            super.onMaxStepsReached(runConfig, lastModelContent, events, seq, step, totalToolCalls);
            return;
        }
        QualityGateResult readiness = evaluateResearchReadiness(runConfig);
        if (readiness != null && !readiness.passed()) {
            events.add(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                    "Research completed with explicit limitations. " + buildLimitationsSummary(readiness),
                    Map.of("stopReason", "MAX_STEPS_REACHED", "qualityGaps", readiness.gaps(),
                            "recommendation", readiness.recommendation())));
        }
    }

    private void advanceDimensionIfNeeded(AgentRunConfig runConfig, List<AgentEvent> events, AtomicInteger seq, int step) {
        ResearchPlan plan = researchPlanStore.findByRunId(runConfig.runId()).orElse(null);
        if (plan == null) return;

        ResearchDimension currentDim = null;
        for (ResearchDimension dim : plan.dimensions()) {
            if (dim.status() == ResearchTaskStatus.IN_PROGRESS) {
                currentDim = dim;
                break;
            }
        }

        if (currentDim != null && currentDim.actualSourceCount() >= currentDim.expectedSourceCount()) {
            researchProgressTracker.markDimensionCompleted(runConfig.runId(), currentDim.id());
            events.add(event(seq, runConfig, AgentEventType.RESEARCH_DIMENSION_COMPLETED,
                    "Dimension completed: " + currentDim.title(),
                    Map.of("dimensionId", currentDim.id(), "dimensionTitle", currentDim.title(),
                            "sourceCount", currentDim.actualSourceCount(), "evidenceCount", currentDim.actualEvidenceCount(),
                            "step", step)));

            for (ResearchDimension dim : plan.dimensions()) {
                if (dim.status() == ResearchTaskStatus.PENDING) {
                    researchProgressTracker.markDimensionStarted(runConfig.runId(), dim.id());
                    events.add(event(seq, runConfig, AgentEventType.RESEARCH_DIMENSION_STARTED,
                            "Dimension started: " + dim.title(),
                            Map.of("dimensionId", dim.id(), "dimensionTitle", dim.title(),
                                     "expectedSourceCount", dim.expectedSourceCount(), "step", step)));
                    break;
                }
            }
        } else if (currentDim == null) {
            for (ResearchDimension dim : plan.dimensions()) {
                if (dim.status() == ResearchTaskStatus.PENDING) {
                    researchProgressTracker.markDimensionStarted(runConfig.runId(), dim.id());
                    events.add(event(seq, runConfig, AgentEventType.RESEARCH_DIMENSION_STARTED,
                            "Dimension started: " + dim.title(),
                            Map.of("dimensionId", dim.id(), "dimensionTitle", dim.title(),
                                     "expectedSourceCount", dim.expectedSourceCount(), "step", step)));
                    break;
                }
            }
        }
    }

    private QualityGateResult evaluateResearchReadiness(AgentRunConfig runConfig) {
        if (this.researchRuntimeSupport == null || this.researchQualityGate == null) {
            return null;
        }
        ResearchPlan plan = this.researchPlanStore == null ? null : this.researchPlanStore.findByRunId(runConfig.runId()).orElse(null);
        List<ResearchSource> sources = this.researchRuntimeSupport.listSourcesByRun(runConfig.runId());
        List<EvidenceItem> evidenceItems = this.researchRuntimeSupport.listEvidenceByRun(runConfig.runId());
        boolean requireCitations = runConfig.researchOptions() == null || Boolean.TRUE.equals(runConfig.researchOptions().requireCitations());
        return this.researchQualityGate.evaluate(plan, sources, evidenceItems, requireCitations);
    }

    private boolean shouldContinueResearch(AgentRunConfig runConfig, QualityGateResult readiness) {
        if (this.researchPlanStore == null || this.researchQualityGate == null || this.researchRuntimeSupport == null) {
            return false;
        }
        if (this.researchPlanStore.findByRunId(runConfig.runId()).isEmpty()) {
            return false;
        }
        return readiness != null && !readiness.passed();
    }

    private String buildContinuationInstruction(AgentRunConfig runConfig, QualityGateResult readiness) {
        ResearchPlan plan = this.researchPlanStore == null ? null : this.researchPlanStore.findByRunId(runConfig.runId()).orElse(null);
        if (plan == null) {
            return "A structured research plan is still missing. Search from multiple angles and gather full-source evidence before finishing.";
        }
        ResearchDimension currentDimension = plan.dimensions().stream()
                .filter(dimension -> dimension.status() == ResearchTaskStatus.IN_PROGRESS || dimension.status() == ResearchTaskStatus.PENDING)
                .findFirst()
                .orElse(null);
        StringBuilder builder = new StringBuilder("Do not finish yet. Continue the research workflow.");
        if (currentDimension != null) {
            builder.append(" Focus on dimension \"").append(currentDimension.title()).append("\".");
            if (!currentDimension.searchQueries().isEmpty()) {
                builder.append(" Suggested queries: ")
                        .append(String.join(" | ", currentDimension.searchQueries().stream().limit(3).toList()))
                        .append(".");
            }
        }
        if (readiness != null && !readiness.gaps().isEmpty()) {
            builder.append(" Remaining gaps: ").append(String.join("; ", readiness.gaps())).append(".");
        }
        return builder.toString();
    }

    private String buildLimitationsSummary(QualityGateResult readiness) {
        if (readiness == null) {
            return "No structured quality assessment was available.";
        }
        String gaps = readiness.gaps().isEmpty() ? "No specific gaps were captured." : String.join("; ", readiness.gaps());
        return "Limitations: " + gaps + ". Recommendation: " + readiness.recommendation();
    }

    private void persistUnifiedClaims(AgentRunConfig runConfig, String finalAnswer) {
        if (this.claimStore == null || this.newEvidenceItemStore == null) {
            return;
        }
        try {
            List<org.wrj.haifa.ai.deerflow.evidence.EvidenceItem> evidence = this.newEvidenceItemStore.findByRunId(runConfig.runId());
            if (evidence.isEmpty() || !this.claimStore.findByRunId(runConfig.runId()).isEmpty()) {
                return;
            }
            List<String> evidenceIds = evidence.stream().map(org.wrj.haifa.ai.deerflow.evidence.EvidenceItem::getEvidenceId).toList();
            var claim = this.claimStore.create(runConfig.runId(), runConfig.threadId(), null,
                    summarizeClaim(finalAnswer), evidenceIds, 0.8, "accepted");
            if (this.citationStore != null) {
                for (var item : evidence) {
                    this.citationStore.create(claim.getClaimId(), item.getEvidenceId(), item.getSourceId(), item.getLocator(), "valid");
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to write unified claims/citations: {}", ex.getMessage());
        }
    }

    private static String summarizeClaim(String finalAnswer) {
        if (finalAnswer == null || finalAnswer.isBlank()) {
            return "Research answer synthesized from collected evidence.";
        }
        String normalized = finalAnswer.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
    private String firstWorkItemId(String runId) {
        if (this.workItemStore == null) {
            return null;
        }
        return this.workItemStore.findByRunId(runId).stream()
                .findFirst()
                .map(item -> item.getWorkItemId())
                .orElse(null);
    }

    private CitationProcessingResult finalizeResearchAnswer(AgentRunConfig runConfig, String answer) {
        if (this.researchRuntimeSupport == null) {
            return new CitationProcessingResult(answer, List.of(), List.of());
        }
        return this.researchRuntimeSupport.finalizeAnswer(runConfig.threadId(), runConfig.runId(), answer);
    }

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata) {
        return AgentEvent.of(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type, content,
                metadata);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String extractJsonValue(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return "";
        }
        String quotedField = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(quotedField);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            return "";
        }
        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return "";
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }
        return json.substring(firstQuote + 1, secondQuote).trim();
    }
}
