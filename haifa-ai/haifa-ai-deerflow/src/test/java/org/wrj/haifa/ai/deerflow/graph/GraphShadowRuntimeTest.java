package org.wrj.haifa.ai.deerflow.graph;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphShadowRuntimeTest {

    @Test
    void runsDryChatGraphWithoutCallingRealModelOrTools() {
        GraphShadowRuntime runtime = new GraphShadowRuntime();
        AgentRunConfig config = new AgentRunConfig("thread-shadow", "run-shadow", "model-a",
                true, false, 4, Path.of("."), RunMode.CHAT, ResearchOptions.defaults(), Map.of());
        AgentRequest request = new AgentRequest("thread-shadow", "hello", "model-a");
        List<MessageRecord> history = List.of(new MessageRecord("m1", "thread-shadow", "run-shadow",
                MessageRole.USER, "hello", Map.of(), Instant.parse("2026-07-01T00:00:00Z")));

        AgentGraphShadowResult result = runtime.run(config, request, history,
                new ModelPrompt("system", "user", "model-a")).block();

        assertThat(result).isNotNull();
        assertThat(result.runId()).isEqualTo("run-shadow");
        assertThat(result.threadId()).isEqualTo("thread-shadow");
        assertThat(result.visitedNodes()).containsExactly(
                "load_context",
                "apply_prompt_middlewares",
                "call_model",
                "parse_tool_calls",
                "finalize"
        );
        AgentGraphStateView view = AgentGraphStateView.of(result.finalState());
        assertThat(view.runId()).isEqualTo("run-shadow");
        assertThat(view.messageWindow()).hasSize(1);
        assertThat(view.modelSteps())
                .extracting(step -> step.get("node"))
                .contains("call_model", "parse_tool_calls", "finalize");
        assertThat(view.list(AgentGraphStateKeys.PENDING_TOOL_CALLS)).isEmpty();
    }
}
