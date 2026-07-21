package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.StateGraph;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.node.CitationVerifierNode;
import org.wrj.haifa.ai.deerflow.graph.node.EvidenceExtractionNode;
import org.wrj.haifa.ai.deerflow.graph.node.ReplannerNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchDimensionDispatchNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchFetchNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchPlannerAgentNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchQualityGateNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchReportNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchSearchNode;
import org.wrj.haifa.ai.deerflow.graph.node.TodoSyncNode;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateStrategies;

@Component
@Deprecated(forRemoval = true)
public class ResearchAgentGraph {

    public static final String GRAPH_NAME = "haifa-active-research";
    public static final String CREATE_OR_LOAD_PLAN = "create_or_load_plan";
    public static final String TODO_SYNC = "todo_sync";
    public static final String DISPATCH_DIMENSIONS = "dispatch_dimensions";
    public static final String SEARCH_SOURCES = "search_sources";
    public static final String FETCH_SOURCES = "fetch_sources";
    public static final String EXTRACT_EVIDENCE = "extract_evidence";
    public static final String QUALITY_GATE = "quality_gate";
    public static final String REPLAN = "replan";
    public static final String VERIFY_CITATIONS = "verify_citations";
    public static final String WRITE_REPORT = "write_report";

    private final ResearchPlannerAgentNode plannerNode;
    private final TodoSyncNode todoSyncNode;
    private final ResearchDimensionDispatchNode dimensionDispatchNode;
    private final ResearchSearchNode searchNode;
    private final ResearchFetchNode fetchNode;
    private final EvidenceExtractionNode evidenceNode;
    private final ResearchQualityGateNode qualityGateNode;
    private final ReplannerNode replannerNode;
    private final CitationVerifierNode citationVerifierNode;
    private final ResearchReportNode reportNode;

    public ResearchAgentGraph(ResearchPlannerAgentNode plannerNode,
                              TodoSyncNode todoSyncNode,
                              ResearchDimensionDispatchNode dimensionDispatchNode,
                              ResearchSearchNode searchNode,
                              ResearchFetchNode fetchNode,
                              EvidenceExtractionNode evidenceNode,
                              ResearchQualityGateNode qualityGateNode,
                              ReplannerNode replannerNode,
                              CitationVerifierNode citationVerifierNode,
                              ResearchReportNode reportNode) {
        this.plannerNode = plannerNode;
        this.todoSyncNode = todoSyncNode;
        this.dimensionDispatchNode = dimensionDispatchNode;
        this.searchNode = searchNode;
        this.fetchNode = fetchNode;
        this.evidenceNode = evidenceNode;
        this.qualityGateNode = qualityGateNode;
        this.replannerNode = replannerNode;
        this.citationVerifierNode = citationVerifierNode;
        this.reportNode = reportNode;
    }

    public StateGraph build(int maxSteps) throws Exception {
        StateGraph graph = new StateGraph(GRAPH_NAME, AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(CREATE_OR_LOAD_PLAN, plannerNode);
        graph.addNode(TODO_SYNC, todoSyncNode);
        graph.addNode(DISPATCH_DIMENSIONS, dimensionDispatchNode);
        graph.addNode(SEARCH_SOURCES, searchNode);
        graph.addNode(FETCH_SOURCES, fetchNode);
        graph.addNode(EXTRACT_EVIDENCE, evidenceNode);
        graph.addNode(QUALITY_GATE, qualityGateNode);
        graph.addNode(REPLAN, replannerNode);
        graph.addNode(VERIFY_CITATIONS, citationVerifierNode);
        graph.addNode(WRITE_REPORT, reportNode);

        graph.addEdge(StateGraph.START, CREATE_OR_LOAD_PLAN)
                .addEdge(CREATE_OR_LOAD_PLAN, TODO_SYNC)
                .addEdge(TODO_SYNC, DISPATCH_DIMENSIONS)
                .addEdge(DISPATCH_DIMENSIONS, SEARCH_SOURCES)
                .addEdge(SEARCH_SOURCES, FETCH_SOURCES)
                .addEdge(FETCH_SOURCES, EXTRACT_EVIDENCE)
                .addEdge(EXTRACT_EVIDENCE, QUALITY_GATE);

        graph.addConditionalEdges(QUALITY_GATE, state -> {
            Boolean passed = (Boolean) state.data().getOrDefault(AgentGraphStateKeys.QUALITY_GATE_PASSED, false);
            Integer steps = (Integer) state.data().getOrDefault(AgentGraphStateKeys.RESEARCH_STEPS, 0);
            if (Boolean.TRUE.equals(passed) || steps >= maxSteps) {
                return CompletableFuture.completedFuture(VERIFY_CITATIONS);
            }
            return CompletableFuture.completedFuture(REPLAN);
        }, Map.of(VERIFY_CITATIONS, VERIFY_CITATIONS, REPLAN, REPLAN));

        graph.addEdge(REPLAN, TODO_SYNC)
                .addEdge(VERIFY_CITATIONS, WRITE_REPORT)
                .addEdge(WRITE_REPORT, StateGraph.END);

        return graph;
    }
}
