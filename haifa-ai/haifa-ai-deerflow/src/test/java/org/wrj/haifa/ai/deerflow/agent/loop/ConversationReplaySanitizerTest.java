package org.wrj.haifa.ai.deerflow.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;

class ConversationReplaySanitizerTest {

    private final ConversationReplaySanitizer sanitizer = new ConversationReplaySanitizer();

    @Test
    void parsesLegacyXmlToolCallAndPairsFollowingToolResult() {
        List<ModelMessage> sanitized = sanitizer.sanitize(List.of(
                new ModelMessage(ModelMessage.Role.USER, "Search it"),
                new ModelMessage(ModelMessage.Role.ASSISTANT,
                        "<tool_call name=\"mock_search\">{\"query\":\"deerflow\"}</tool_call>"),
                new ModelMessage(ModelMessage.Role.TOOL, "result text", List.of(), null, "mock_search", Map.of())
        ));

        assertThat(sanitized).hasSize(3);
        ModelMessage assistant = sanitized.get(1);
        ModelMessage tool = sanitized.get(2);

        assertThat(assistant.role()).isEqualTo(ModelMessage.Role.ASSISTANT);
        assertThat(assistant.toolCalls()).hasSize(1);
        assertThat(assistant.toolCalls().getFirst().name()).isEqualTo("mock_search");
        assertThat(tool.role()).isEqualTo(ModelMessage.Role.TOOL);
        assertThat(tool.toolCallId()).isEqualTo(assistant.toolCalls().getFirst().id());
        assertThat(tool.name()).isEqualTo("mock_search");
    }

    @Test
    void insertsSyntheticToolResultForMissingAssistantToolCallResult() {
        List<ModelMessage> sanitized = sanitizer.sanitize(List.of(
                new ModelMessage(ModelMessage.Role.USER, "Run tool"),
                new ModelMessage(
                        ModelMessage.Role.ASSISTANT,
                        "",
                        List.of(new ModelToolCall("call-1", "mock_search", "{\"query\":\"x\"}")),
                        null,
                        null,
                        Map.of())
        ));

        assertThat(sanitized).hasSize(3);
        ModelMessage synthetic = sanitized.get(2);
        assertThat(synthetic.role()).isEqualTo(ModelMessage.Role.TOOL);
        assertThat(synthetic.toolCallId()).isEqualTo("call-1");
        assertThat(synthetic.name()).isEqualTo("mock_search");
        assertThat(synthetic.content()).isEqualTo("aborted");
        assertThat(synthetic.metadata()).containsEntry("synthetic", true);
    }

    @Test
    void dropsStrayToolResults() {
        List<ModelMessage> sanitized = sanitizer.sanitize(List.of(
                new ModelMessage(ModelMessage.Role.USER, "Hello"),
                new ModelMessage(ModelMessage.Role.TOOL, "orphan", List.of(), "missing", "mock_search", Map.of()),
                new ModelMessage(ModelMessage.Role.ASSISTANT, "Hi")
        ));

        assertThat(sanitized).extracting(ModelMessage::role)
                .containsExactly(ModelMessage.Role.USER, ModelMessage.Role.ASSISTANT);
    }
}
