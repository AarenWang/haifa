package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.GraphCheckpointRecorder;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.SQLiteCheckpointSaver;
import org.wrj.haifa.ai.deerflow.graph.node.*;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateFactory;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateStrategies;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphChatRuntime {

    private static final String GRAPH_NAME = "haifa-active-chat";
    private static final String LOAD_CONTEXT = "load_context";
    private static final String APPLY_PROMPT_MIDDLEWARES = "apply_prompt_middlewares";
    private static final String CALL_MODEL = "call_model";
    private static final String PARSE_MODEL_OUTPUT = "parse_model_output";
    private static final String EXECUTE_TOOLS = "execute_tools";
    private static final String FINAL_ANSWER_GATE = "final_answer_gate";
    private static final String FINALIZE = "finalize";
    private static final String APPROVAL_GATE = "approval_gate";
    private static final String CLARIFICATION_GATE = "clarification_gate";

    private final DeerFlowProperties properties;
    private final GraphCheckpointRecorder checkpointRecorder;
    private final AgentGraphStateFactory stateFactory;
    private final SQLiteCheckpointSaver sqliteCheckpointSaver;

    private final ChatLoadContextNode loadContextNode;
    private final ChatApplyMiddlewaresNode applyMiddlewaresNode;
    private final ChatCallModelNode callModelNode;
    private final ChatParseModelOutputNode parseModelOutputNode;
    private final ChatExecuteToolsNode executeToolsNode;
    private final ChatFinalAnswerGateNode finalAnswerGateNode;
    private final ChatFinalizeNode finalizeNode;
    private final ApprovalGateNode approvalGateNode;
    private final ClarificationGateNode clarificationGateNode;

    public GraphChatRuntime() {
        this(new DeerFlowProperties(), null, new AgentGraphStateFactory(), null, null, null, null, null, null, null, null, null, null);
    }

    public GraphChatRuntime(DeerFlowProperties properties, GraphCheckpointRecorder checkpointRecorder) {
        this(properties, checkpointRecorder, new AgentGraphStateFactory(), null, null, null, null, null, null, null, null, null, null);
    }

    @Autowired
    public GraphChatRuntime(DeerFlowProperties properties,
                            GraphCheckpointRecorder checkpointRecorder,
                            AgentGraphStateFactory stateFactory,
                            SQLiteCheckpointSaver sqliteCheckpointSaver,
                            ChatLoadContextNode loadContextNode,
                            ChatApplyMiddlewaresNode applyMiddlewaresNode,
                            ChatCallModelNode callModelNode,
                            ChatParseModelOutputNode parseModelOutputNode,
                            ChatExecuteToolsNode executeToolsNode,
                            ChatFinalAnswerGateNode finalAnswerGateNode,
                            ChatFinalizeNode finalizeNode,
                            ApprovalGateNode approvalGateNode,
                            ClarificationGateNode clarificationGateNode) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.checkpointRecorder = checkpointRecorder;
        this.stateFactory = stateFactory == null ? new AgentGraphStateFactory() : stateFactory;
        this.sqliteCheckpointSaver = sqliteCheckpointSaver;
        this.loadContextNode = loadContextNode;
        this.applyMiddlewaresNode = applyMiddlewaresNode;
        this.callModelNode = callModelNode;
        this.parseModelOutputNode = parseModelOutputNode;
        this.executeToolsNode = executeToolsNode;
        this.finalAnswerGateNode = finalAnswerGateNode;
        this.finalizeNode = finalizeNode;
        this.approvalGateNode = approvalGateNode;
        this.clarificationGateNode = clarificationGateNode;
    }

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    public Flux<AgentEvent> run(GraphChatRuntimeRequest request) {
        Sinks.Many<AgentEvent> eventSink = Sinks.many().unicast().onBackpressureBuffer();
        String runId = request.runConfig().runId();
        GraphEventRegistry.register(runId, eventSink, request.eventSequence());

        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        CompletableFuture.runAsync(() -> {
            try {
                GraphChatLifecycleRegistry.register(runId, new GraphChatLifecycleContext(
                        request.runConfig(),
                        request.loopConfig(),
                        request.agentLoop() == null ? null : request.agentLoop().observer(),
                        request.eventSequence(),
                        request.activeSkills(),
                        request.uploadedFileIds()
                ));

                Map<String, Object> initialState = new HashMap<>(stateFactory.create(
                        request.runConfig(),
                        request.agentRequest(),
                        request.threadHistory(),
                        new ModelPrompt(request.systemPrompt(), request.userPrompt(), request.runConfig().modelName()),
                        request.activeSkills()
                ));
                initialState.put("chat_steps", 0);
                initialState.put("last_assistant_content", "");

                BaseCheckpointSaver saver = checkpointEnabled() ? sqliteCheckpointSaver : null;
                int maxIterations = request.loopConfig().maxSteps();
                CompiledGraph graph = saver == null ? chatGraph(maxIterations).compile() : chatGraph(maxIterations).compile(
                        CompileConfig.builder()
                                .recursionLimit(maxIterations * 5 + 5)
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .build());

                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(request.runConfig().threadId())
                        .build();
                runnableConfig.context().put("runId", runId);
                runnableConfig.context().put("graphName", GRAPH_NAME);

                boolean isResume = false;
                if (saver != null) {
                    var latestCp = saver.get(runnableConfig);
                    if (latestCp.isPresent() && latestCp.get().getNextNodeId() != null && !latestCp.get().getNextNodeId().isBlank()) {
                        isResume = true;
                    }
                }

                var streamResult = isResume
                        ? graph.stream(Map.of(AgentGraphStateKeys.RUN_ID, runId), runnableConfig)
                        : graph.stream(initialState, runnableConfig);

                streamResult.collectList()
                        .block(Duration.ofMillis(request.loopConfig().timeoutMs()));

                eventSink.tryEmitComplete();
            }
            catch (Exception ex) {
                eventSink.tryEmitError(ex);
            }
            finally {
                GraphChatLifecycleRegistry.deregister(runId);
                GraphEventRegistry.deregister(runId);
            }
        }, executor);

        return eventSink.asFlux();
    }

    private StateGraph chatGraph(int maxSteps) throws Exception {
        StateGraph graph = new StateGraph(GRAPH_NAME, AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(LOAD_CONTEXT, loadContextNode);
        graph.addNode(APPLY_PROMPT_MIDDLEWARES, applyMiddlewaresNode);
        graph.addNode(CALL_MODEL, callModelNode);
        graph.addNode(PARSE_MODEL_OUTPUT, parseModelOutputNode);
        graph.addNode(EXECUTE_TOOLS, executeToolsNode);
        graph.addNode(FINAL_ANSWER_GATE, finalAnswerGateNode);
        graph.addNode(FINALIZE, finalizeNode);
        graph.addNode(APPROVAL_GATE, approvalGateNode);
        graph.addNode(CLARIFICATION_GATE, clarificationGateNode);

        graph.addEdge(StateGraph.START, LOAD_CONTEXT)
                .addEdge(LOAD_CONTEXT, APPLY_PROMPT_MIDDLEWARES)
                .addEdge(APPLY_PROMPT_MIDDLEWARES, CALL_MODEL)
                .addEdge(CALL_MODEL, PARSE_MODEL_OUTPUT);

        graph.addConditionalEdges(PARSE_MODEL_OUTPUT, state -> {
            List<Map<String, Object>> pending = AgentGraphStateView.of(state).listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);
            Integer steps = (Integer) state.data().getOrDefault("chat_steps", 0);
            if (pending == null || pending.isEmpty() || steps >= maxSteps) {
                return CompletableFuture.completedFuture(FINAL_ANSWER_GATE);
            }
            return CompletableFuture.completedFuture(APPROVAL_GATE);
        }, Map.of(FINAL_ANSWER_GATE, FINAL_ANSWER_GATE, APPROVAL_GATE, APPROVAL_GATE));

        graph.addConditionalEdges(APPROVAL_GATE, state -> {
            String status = (String) state.data().get("approval_gate_status");
            if ("SUSPEND".equals(status)) {
                return CompletableFuture.completedFuture(StateGraph.END);
            } else if ("DENIED".equals(status)) {
                return CompletableFuture.completedFuture(CALL_MODEL);
            }
            return CompletableFuture.completedFuture(EXECUTE_TOOLS);
        }, Map.of(StateGraph.END, StateGraph.END, CALL_MODEL, CALL_MODEL, EXECUTE_TOOLS, EXECUTE_TOOLS));

        graph.addConditionalEdges(EXECUTE_TOOLS, state -> {
            Map<String, Object> clarMeta = (Map<String, Object>) state.data().get("clarification_metadata");
            if (clarMeta != null && !clarMeta.isEmpty()) {
                return CompletableFuture.completedFuture(CLARIFICATION_GATE);
            }
            return CompletableFuture.completedFuture(CALL_MODEL);
        }, Map.of(CLARIFICATION_GATE, CLARIFICATION_GATE, CALL_MODEL, CALL_MODEL));

        graph.addConditionalEdges(FINAL_ANSWER_GATE, state -> {
            String status = (String) state.data().get("final_answer_gate_status");
            if ("CONTINUE".equals(status)) {
                return CompletableFuture.completedFuture(CALL_MODEL);
            }
            return CompletableFuture.completedFuture(FINALIZE);
        }, Map.of(CALL_MODEL, CALL_MODEL, FINALIZE, FINALIZE));

        graph.addEdge(CLARIFICATION_GATE, StateGraph.END);
        graph.addEdge(FINALIZE, StateGraph.END);

        return graph;
    }

    private boolean checkpointEnabled() {
        return properties.getGraph() != null
                && properties.getGraph().getCheckpoint() != null
                && properties.getGraph().getCheckpoint().isEnabled()
                && sqliteCheckpointSaver != null;
    }
}
