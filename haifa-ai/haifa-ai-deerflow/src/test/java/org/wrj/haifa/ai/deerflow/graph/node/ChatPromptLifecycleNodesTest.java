package org.wrj.haifa.ai.deerflow.graph.node;

import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.cloud.ai.graph.OverAllState;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionLimits;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.NoopExecutionHook;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContext;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContextRegistry;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareLifecycle;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewarePhase;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;
import org.springframework.test.util.ReflectionTestUtils;

class ChatPromptLifecycleNodesTest {

    private static final String RUN_ID = "run-prompt-lifecycle";
    private final RunExecutionContextRegistry contextRegistry = new RunExecutionContextRegistry();

    @AfterEach
    void cleanupLifecycle() {
        contextRegistry.close(RUN_ID);
    }

    @Test
    void preparesOnceAndReassemblesBeforeEveryModelStepWithoutAccumulation() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setModelTimeout(10_000);
        RunOnceMiddleware runOnce = new RunOnceMiddleware();
        ModelStepMiddleware modelStep = new ModelStepMiddleware();
        GraphExecutionManager executionManager = new GraphExecutionManager(new GraphExecutorProperties());

        ChatPrepareRunNode prepareNode = new ChatPrepareRunNode(
                properties, List.of(modelStep, runOnce), executionManager);
        ChatAssembleModelInputNode assembleNode = new ChatAssembleModelInputNode(
                properties, List.of(modelStep, runOnce), executionManager);
        ReflectionTestUtils.setField(prepareNode, "executionContextRegistry", contextRegistry);
        ReflectionTestUtils.setField(assembleNode, "executionContextRegistry", contextRegistry);

        AgentRunConfig config = new AgentRunConfig(
                "thread-prompt-lifecycle", RUN_ID, "test-model", false, false, 4,
                Path.of("."), RunMode.CHAT, ResearchOptions.defaults(), Map.of());
        AgentRequest request = new AgentRequest(config.threadId(), "hello", config.modelName());
        contextRegistry.register(RUN_ID, new RunExecutionContext(config, request,
                new ExecutionLimits(4, 4, 30_000, ResearchOptions.defaults()), NoopExecutionHook.INSTANCE,
                new AtomicInteger(), null, List.of(), List.of(), java.util.Set.of(), "", "", 0));

        Map<String, Object> state = new HashMap<>();
        state.put(AgentGraphStateKeys.RUN_ID, RUN_ID);
        state.put(AgentGraphStateKeys.THREAD_ID, config.threadId());
        state.put(AgentGraphStateKeys.MODEL_NAME, config.modelName());
        state.put(AgentGraphStateKeys.RUN_PREPARED, false);
        state.put(AgentGraphStateKeys.RUN_PROMPT_BASE, Map.of());
        state.put(AgentGraphStateKeys.PROMPT_REVISION, 0);
        state.put(AgentGraphStateKeys.TOOL_RESULTS, List.of());

        Map<String, Object> firstPrepare = prepareNode.apply(new OverAllState(state)).join();
        state.putAll(firstPrepare);
        Map<String, Object> secondPrepare = prepareNode.apply(new OverAllState(state)).join();

        assertThat(runOnce.invocations()).isEqualTo(1);
        assertThat(secondPrepare.get(AgentGraphStateKeys.MODEL_STEPS).toString())
                .contains("already_prepared");

        Map<String, Object> firstAssembly = assembleNode.apply(new OverAllState(state)).join();
        state.putAll(firstAssembly);
        Map<String, Object> secondAssembly = assembleNode.apply(new OverAllState(state)).join();

        assertThat(modelStep.invocations()).isEqualTo(2);
        assertThat(firstAssembly.get(AgentGraphStateKeys.PROMPT_REVISION)).isEqualTo(1);
        assertThat(secondAssembly.get(AgentGraphStateKeys.PROMPT_REVISION)).isEqualTo(2);
        String secondSystemPrompt = ((Map<?, ?>) secondAssembly.get(AgentGraphStateKeys.MODEL_PROMPT))
                .get("systemPrompt").toString();
        assertThat(secondSystemPrompt).contains("[run-prepared]").contains("[model-step]");
        assertThat(count(secondSystemPrompt, "[run-prepared]")).isEqualTo(1);
        assertThat(count(secondSystemPrompt, "[model-step]")).isEqualTo(1);
    }

    private static int count(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    @MiddlewareLifecycle(MiddlewarePhase.RUN_PREPARATION)
    @MiddlewareOrder(10)
    private static final class RunOnceMiddleware implements AgentMiddleware {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
            invocations.incrementAndGet();
            return next.next(context).map(prompt -> new ModelPrompt(
                    prompt.systemPrompt() + "\n[run-prepared]",
                    prompt.userPrompt(), prompt.modelName()));
        }

        int invocations() {
            return invocations.get();
        }
    }

    @MiddlewareLifecycle(MiddlewarePhase.MODEL_INPUT)
    @MiddlewareOrder(10)
    private static final class ModelStepMiddleware implements AgentMiddleware {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
            invocations.incrementAndGet();
            return next.next(context).map(prompt -> new ModelPrompt(
                    prompt.systemPrompt() + "\n[model-step]",
                    prompt.userPrompt(), prompt.modelName()));
        }

        int invocations() {
            return invocations.get();
        }
    }
}
