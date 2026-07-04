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
    private static final String FINALIZE = "finalize";

    private final DeerFlowProperties properties;
    private final GraphCheckpointRecorder checkpointRecorder;
    private final AgentGraphStateFactory stateFactory;
    private final SQLiteCheckpointSaver sqliteCheckpointSaver;

    private final ChatLoadContextNode loadContextNode;
    private final ChatApplyMiddlewaresNode applyMiddlewaresNode;
    private final ChatCallModelNode callModelNode;
    private final ChatParseModelOutputNode parseModelOutputNode;
    private final ChatExecuteToolsNode executeToolsNode;
    private final ChatFinalizeNode finalizeNode;

    public GraphChatRuntime() {
        this(new DeerFlowProperties(), null, new AgentGraphStateFactory(), null, null, null, null, null, null, null);
    }

    public GraphChatRuntime(DeerFlowProperties properties, GraphCheckpointRecorder checkpointRecorder) {
        this(properties, checkpointRecorder, new AgentGraphStateFactory(), null, null, null, null, null, null, null);
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
                            ChatFinalizeNode finalizeNode) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.checkpointRecorder = checkpointRecorder;
        this.stateFactory = stateFactory == null ? new AgentGraphStateFactory() : stateFactory;
        this.sqliteCheckpointSaver = sqliteCheckpointSaver;
        this.loadContextNode = loadContextNode;
        this.applyMiddlewaresNode = applyMiddlewaresNode;
        this.callModelNode = callModelNode;
        this.parseModelOutputNode = parseModelOutputNode;
        this.executeToolsNode = executeToolsNode;
        this.finalizeNode = finalizeNode;
    }

    public Flux<AgentEvent> run(GraphChatRuntimeRequest request) {
        Sinks.Many<AgentEvent> eventSink = Sinks.many().unicast().onBackpressureBuffer();
        String runId = request.runConfig().runId();
        GraphEventRegistry.register(runId, eventSink, request.eventSequence());

        CompletableFuture.runAsync(() -> {
            try {
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
                GraphEventRegistry.deregister(runId);
            }
        });

        return eventSink.asFlux();
    }

    private StateGraph chatGraph(int maxSteps) throws Exception {
        StateGraph graph = new StateGraph(GRAPH_NAME, AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(LOAD_CONTEXT, loadContextNode);
        graph.addNode(APPLY_PROMPT_MIDDLEWARES, applyMiddlewaresNode);
        graph.addNode(CALL_MODEL, callModelNode);
        graph.addNode(PARSE_MODEL_OUTPUT, parseModelOutputNode);
        graph.addNode(EXECUTE_TOOLS, executeToolsNode);
        graph.addNode(FINALIZE, finalizeNode);

        graph.addEdge(StateGraph.START, LOAD_CONTEXT)
                .addEdge(LOAD_CONTEXT, APPLY_PROMPT_MIDDLEWARES)
                .addEdge(APPLY_PROMPT_MIDDLEWARES, CALL_MODEL)
                .addEdge(CALL_MODEL, PARSE_MODEL_OUTPUT);

        graph.addConditionalEdges(PARSE_MODEL_OUTPUT, state -> {
            List<Map<String, Object>> pending = AgentGraphStateView.of(state).listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);
            Integer steps = (Integer) state.data().getOrDefault("chat_steps", 0);
            if (pending == null || pending.isEmpty() || steps >= maxSteps) {
                return CompletableFuture.completedFuture(FINALIZE);
            } else {
                return CompletableFuture.completedFuture(EXECUTE_TOOLS);
            }
        }, Map.of(FINALIZE, FINALIZE, EXECUTE_TOOLS, EXECUTE_TOOLS));

        graph.addEdge(EXECUTE_TOOLS, CALL_MODEL);
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
