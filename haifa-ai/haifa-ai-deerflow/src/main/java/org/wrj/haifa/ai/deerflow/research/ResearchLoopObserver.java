package org.wrj.haifa.ai.deerflow.research;

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

/**
 * AgentLoopObserver implementation for research-specific logic.
 */
public class ResearchLoopObserver extends DefaultAgentLoopObserver {

    private static final Logger log = LoggerFactory.getLogger(ResearchLoopObserver.class);

    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final ResearchPlanner researchPlanner;
    private final ResearchPlanStore researchPlanStore;
    private final ResearchProgressTracker researchProgressTracker;
    private final ResearchQualityGate researchQualityGate;

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

    @Override
    public ToolCallResult beforeToolExecute(AgentRunConfig runConfig, ToolCall toolCall) {
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
        if (runConfig.mode() != RunMode.RESEARCH) {
            return super.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
        }
        if (this.researchRuntimeSupport == null || toolResult.status() != ToolCallResult.Status.SUCCESS) {
            return null;
        }

        if ("web_search".equals(toolCall.toolName())) {
            SearchIngestionResult ingestionResult = this.researchRuntimeSupport.ingestSearchResults(
                    runConfig.threadId(), runConfig.runId(), toolResult.result());
            for (var registration : ingestionResult.registrations()) {
                events.add(event(seq, runConfig, AgentEventType.SOURCE_FOUND,
                        registration.source().title(),
                        Map.of("sourceId", registration.source().sourceId(),
                                "url", registration.source().url(),
                                "domain", registration.source().domain(),
                                "deduplicated", registration.deduplicated())));
            }
            return ingestionResult.observation();
        }

        if ("web_fetch".equals(toolCall.toolName())) {
            String url = stringValue(toolResult.metadata().get("url"));
            if (url.isBlank()) {
                url = extractJsonValue(toolCall.arguments(), "url");
            }
            if (url.isBlank()) {
                return null;
            }
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
            }
            return fetchProcessingResult.observation();
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
    public FinalAnswerResult onFinalAnswerAccepted(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        // Run default checks (e.g. todo items completeness and limitations appending)
        FinalAnswerResult baseResult = super.onFinalAnswerAccepted(runConfig, rawAnswer, events, seq, step, totalToolCalls);

        if (runConfig.mode() != RunMode.RESEARCH) {
            return baseResult;
        }

        CitationProcessingResult citationProcessingResult = finalizeResearchAnswer(runConfig, baseResult.finalAnswer());
        Map<String, Object> extraMetadata = Map.of(
                "citedSources", citationProcessingResult.citedSources().stream().map(ResearchSource::sourceId).toList(),
                "citedEvidence", citationProcessingResult.citations().stream().flatMap(c -> c.evidenceIds().stream()).distinct().toList()
        );
        return new FinalAnswerResult(citationProcessingResult.finalAnswer(), extraMetadata);
    }

    @Override
    public void onMaxStepsReached(AgentRunConfig runConfig, String lastModelContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
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
            return true;
        }
        return readiness == null || !readiness.passed();
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
