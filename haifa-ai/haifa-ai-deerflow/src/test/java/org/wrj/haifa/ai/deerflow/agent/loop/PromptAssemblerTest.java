package org.wrj.haifa.ai.deerflow.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;

class PromptAssemblerTest {

    @Test
    void assemblesSanitizedTypedMessagesBeforeModelCall() {
        PromptAssembler assembler = new PromptAssembler();

        PromptAssembler.Result result = assembler.assemble("stable system", "model-a", List.of(
                new ModelMessage(ModelMessage.Role.USER, "Need data"),
                new ModelMessage(ModelMessage.Role.ASSISTANT, "", List.of(
                        new ModelToolCall("call-1", "mock_search", "{\"query\":\"haifa\"}")
                ), null, null, Map.of()),
                new ModelMessage(ModelMessage.Role.TOOL, "found result", List.of(), "call-1", "mock_search", Map.of())
        ));

        assertThat(result.prompt().hasMessages()).isTrue();
        assertThat(result.prompt().messages()).hasSize(3);
        assertThat(result.prompt().userPrompt()).contains("User: Need data");
        assertThat(result.prompt().userPrompt()).contains("<tool_call name=\"mock_search\">");
        assertThat(result.prompt().userPrompt()).contains("Tool result (mock_search) [call-1]: found result");
        assertThat(result.trace().messageCount()).isEqualTo(3);
        assertThat(result.trace().estimatedTokens()).isGreaterThan(0);
    }
}
