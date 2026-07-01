package org.wrj.haifa.ai.deerflow.tool;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentClarificationStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStatus;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AskClarificationToolTest {

    private final AgentClarificationStore store = new AgentClarificationStore();
    private final AskClarificationTool tool = new AskClarificationTool(store);

    @Test
    void testExecuteValidArguments() {
        String jsonArgs = "{\"question\": \"Are you sure?\", \"clarification_type\": \"ambiguous_requirement\", \"context\": \"some context\"}";
        ToolRequest request = new ToolRequest(jsonArgs, Path.of("."), List.of(), "thread-1", "run-1", RunMode.CHAT, List.of());

        ToolResult result = tool.execute(request);

        assertThat(result.content()).contains("Clarification requested from user");
        assertThat(result.metadata()).isNotNull();
        assertThat(result.metadata().get("clarificationRequired")).isEqualTo(true);
        assertThat(result.metadata().get("question")).isEqualTo("Are you sure?");
        assertThat(result.metadata().get("clarificationType")).isEqualTo("ambiguous_requirement");

        String clarificationId = (String) result.metadata().get("clarificationId");
        assertThat(clarificationId).isNotBlank();

        // Verify stored in clarification store
        var recordOpt = store.findPending("thread-1");
        assertThat(recordOpt).isPresent();
        ClarificationRecord record = recordOpt.get();
        assertThat(record.clarificationId()).isEqualTo(clarificationId);
        assertThat(record.status()).isEqualTo(ClarificationStatus.PENDING);
        assertThat(record.question()).isEqualTo("Are you sure?");
        assertThat(record.clarificationType()).isEqualTo("ambiguous_requirement");
        assertThat(record.context()).isEqualTo("some context");
    }

    @Test
    void testExecuteMissingQuestionReturnsError() {
        String jsonArgs = "{\"clarification_type\": \"ambiguous_requirement\"}";
        ToolRequest request = new ToolRequest(jsonArgs, Path.of("."), List.of(), "thread-1", "run-1", RunMode.CHAT, List.of());

        ToolResult result = tool.execute(request);
        assertThat(result.content()).contains("Error: question is required");
    }

    @Test
    void testExecuteInvalidJsonReturnsError() {
        String jsonArgs = "{invalid_json}";
        ToolRequest request = new ToolRequest(jsonArgs, Path.of("."), List.of(), "thread-1", "run-1", RunMode.CHAT, List.of());

        ToolResult result = tool.execute(request);
        assertThat(result.content()).contains("Error parsing tool arguments as JSON");
    }
}
