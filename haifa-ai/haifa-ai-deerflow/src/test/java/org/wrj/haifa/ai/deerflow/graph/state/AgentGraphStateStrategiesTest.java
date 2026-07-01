package org.wrj.haifa.ai.deerflow.graph.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentGraphStateStrategiesTest {

    @Test
    void appendsListsMergesMapsAndReplacesScalars() {
        Map<String, Object> initial = Map.of(
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of(Map.of("messageId", "m1")),
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of("toolCallId", "tc1")),
                AgentGraphStateKeys.TODOS, Map.of("todo-1", Map.of("status", "pending")),
                AgentGraphStateKeys.USAGE, Map.of("inputTokens", 10),
                AgentGraphStateKeys.FINAL_ANSWER, "old"
        );
        Map<String, Object> update = Map.of(
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of(Map.of("messageId", "m2")),
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of("toolCallId", "tc2")),
                AgentGraphStateKeys.TODOS, Map.of("todo-2", Map.of("status", "completed")),
                AgentGraphStateKeys.USAGE, Map.of("outputTokens", 20),
                AgentGraphStateKeys.FINAL_ANSWER, "new"
        );

        Map<String, Object> merged = OverAllState.updateState(initial, update,
                AgentGraphStateStrategies.keyStrategyFactory().apply());

        assertThat(AgentGraphStateView.of(merged).messageWindow())
                .extracting(message -> message.get("messageId"))
                .containsExactly("m1", "m2");
        assertThat(AgentGraphStateView.of(merged).toolResults())
                .extracting(result -> result.get("toolCallId"))
                .containsExactly("tc1", "tc2");
        assertThat(AgentGraphStateView.of(merged).map(AgentGraphStateKeys.TODOS))
                .containsKeys("todo-1", "todo-2");
        assertThat(AgentGraphStateView.of(merged).map(AgentGraphStateKeys.USAGE))
                .containsEntry("inputTokens", 10)
                .containsEntry("outputTokens", 20);
        assertThat(AgentGraphStateView.of(merged).finalAnswer()).isEqualTo("new");
    }
}
