package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriteResult;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriterService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphLifecycleService;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ResearchReportNode implements AsyncNodeAction {

    private final ReportWriterService reportWriterService;
    private final AgentModelClient modelClient;
    private final ResearchPlanStore planStore;
    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final GraphLifecycleService graphLifecycleService;
    private final DeerFlowProperties properties;

    public ResearchReportNode(ReportWriterService reportWriterService,
                              AgentModelClient modelClient,
                              ResearchPlanStore planStore,
                              ResearchRuntimeSupport researchRuntimeSupport,
                              GraphLifecycleService graphLifecycleService) {
        this(reportWriterService, modelClient, planStore, researchRuntimeSupport, graphLifecycleService, null);
    }

    @Autowired
    public ResearchReportNode(ReportWriterService reportWriterService,
                              AgentModelClient modelClient,
                              ResearchPlanStore planStore,
                              ResearchRuntimeSupport researchRuntimeSupport,
                              GraphLifecycleService graphLifecycleService,
                              DeerFlowProperties properties) {
        this.reportWriterService = reportWriterService;
        this.modelClient = modelClient;
        this.planStore = planStore;
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.graphLifecycleService = graphLifecycleService;
        this.properties = properties == null ? new DeerFlowProperties() : properties;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            // Idempotency: if artifacts already exist, reuse them
            List<Map<String, Object>> existingArtifacts = AgentGraphStateView.of(state).artifacts();
            if (!existingArtifacts.isEmpty()) {
                Map<String, Object> update = new HashMap<>();
                update.put(AgentGraphStateKeys.FINAL_ANSWER, state.<String>value(AgentGraphStateKeys.FINAL_ANSWER).orElse(""));
                update.put(AgentGraphStateKeys.RESEARCH_PHASE, "completed");
                update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "write_report", "status", "reused")));
                return update;
            }

            ResearchOptions options = ResearchNodeStateSupport.researchOptions(
                    state.data().get(AgentGraphStateKeys.RESEARCH_OPTIONS));

            ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
            List<ResearchSource> sources = researchRuntimeSupport.listSourcesByRun(runId);
            List<EvidenceItem> evidenceItems = researchRuntimeSupport.listEvidenceByRun(runId);

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.REPORT_STARTED,
                    "Generating final research report",
                    Map.of()
            ));

            String topic = state.<String>value(AgentGraphStateKeys.USER_MESSAGE).orElse("");
            String synthesisPrompt = buildSynthesisPrompt(topic, sources, evidenceItems);
            ModelPrompt modelPrompt = new ModelPrompt(
                    "You are a research synthesis assistant. Generate a detailed, professional, evidence-backed answer summarizing all research findings. Use inline citations like [1] or [2] matching the sources.",
                    synthesisPrompt,
                    state.<String>value(AgentGraphStateKeys.MODEL_NAME).orElse(null)
            );
            ModelResponse modelResponse = modelClient.generate(modelPrompt)
                    .block(java.time.Duration.ofMillis(properties.getModelTimeout()));
            String synthesisResult = modelResponse != null ? modelResponse.content() : "No synthesis generated.";

            ReportWriteResult result = reportWriterService.writeReport(
                    threadId, runId, plan, sources, evidenceItems, null,
                    synthesisResult, options
            );

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.ARTIFACT_CREATED,
                    "Research report artifact created: " + result.artifact().filename(),
                    Map.of("artifactId", result.artifact().artifactId(),
                           "filename", result.artifact().filename(),
                           "mimeType", result.artifact().mimeType(),
                           "size", result.artifact().size())
            ));

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.REPORT_COMPLETED,
                    "Research report generation completed: " + result.artifact().filename(),
                    Map.of("artifactId", result.artifact().artifactId(),
                           "filename", result.artifact().filename())
            ));

            // Complete research lifecycle side effects using GraphLifecycleService
            graphLifecycleService.completeResearch(runId, threadId, result, sources.size());

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.FINAL_ANSWER, synthesisResult);
            update.put(AgentGraphStateKeys.ARTIFACTS, List.of(Map.of(
                    "artifactId", result.artifact().artifactId(),
                    "filename", result.artifact().filename()
            )));
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "completed");
            update.put(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, sources.size());
            update.put(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, evidenceItems.size());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "write_report", "status", "completed")));
            return update;
        }, executor);
    }

    private String buildSynthesisPrompt(String topic, List<ResearchSource> sources, List<EvidenceItem> evidenceItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("Research Topic: ").append(topic).append("\n\n");
        sb.append("Sources:\n");
        for (int i = 0; i < sources.size(); i++) {
            ResearchSource src = sources.get(i);
            sb.append("[").append(i + 1).append("] ").append(src.title()).append(" (").append(src.url()).append(")\n");
        }
        sb.append("\nEvidence gathered:\n");
        for (EvidenceItem item : evidenceItems) {
            sb.append("- ").append(item.claim()).append(" (Source: ").append(item.sourceId()).append(")\n");
        }
        sb.append("\nInstructions: Synthesize the above evidence into a coherent, deep report answering the research topic. Use inline citations format like [1]. Keep the response structured, clear, and comprehensive.");
        return sb.toString();
    }
}
