package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriteResult;
import org.wrj.haifa.ai.deerflow.artifact.ReportWriterService;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
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

    public ResearchReportNode(ReportWriterService reportWriterService,
                              AgentModelClient modelClient,
                              ResearchPlanStore planStore,
                              ResearchRuntimeSupport researchRuntimeSupport) {
        this.reportWriterService = reportWriterService;
        this.modelClient = modelClient;
        this.planStore = planStore;
        this.researchRuntimeSupport = researchRuntimeSupport;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            ResearchOptions options = (ResearchOptions) state.data().get("researchOptions");
            if (options == null) {
                options = ResearchOptions.defaults();
            }

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
            ModelResponse modelResponse = modelClient.generate(modelPrompt).block();
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

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.FINAL_ANSWER, synthesisResult);
            update.put(AgentGraphStateKeys.ARTIFACTS, List.of(Map.of(
                    "artifactId", result.artifact().artifactId(),
                    "filename", result.artifact().filename()
            )));
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "completed");
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "write_report", "status", "completed")));
            return update;
        });
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
