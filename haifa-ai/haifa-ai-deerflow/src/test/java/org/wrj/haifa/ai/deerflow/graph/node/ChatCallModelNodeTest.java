package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ChatCallModelNodeTest {

    @Test
    void usesModelPromptFromGraphStateAsPrimarySystemPrompt() {
        AtomicReference<ModelPrompt> capturedPrompt = new AtomicReference<>();
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("properties system prompt should not be used");
        ChatCallModelNode node = new ChatCallModelNode(
                prompt -> {
                    capturedPrompt.set(prompt);
                    return Mono.just(new ModelResponse("<final_answer>ok</final_answer>"));
                },
                new ToolRegistry(List.of()),
                properties
        );

        Map<String, Object> update = node.apply(new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-1",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                AgentGraphStateKeys.MODE, RunMode.CHAT.name(),
                AgentGraphStateKeys.MODEL_NAME, "state-model",
                AgentGraphStateKeys.MODEL_PROMPT, Map.of(
                        "systemPrompt", "middleware persona memory skill prompt",
                        "userPrompt", "user prompt from middleware",
                        "modelName", "prompt-model"
                ),
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of(Map.of(
                        "role", ModelMessage.Role.USER.name(),
                        "content", "raw user prompt before middleware"
                ))
        ))).join();

        assertThat(update).containsKey(AgentGraphStateKeys.MESSAGE_WINDOW);
        ModelPrompt prompt = capturedPrompt.get();
        assertThat(prompt).isNotNull();
        assertThat(prompt.systemPrompt())
                .contains("middleware persona memory skill prompt")
                .contains("<tool_call name=\"tool_name\">")
                .doesNotContain("properties system prompt should not be used");
        assertThat(prompt.modelName()).isEqualTo("prompt-model");
        assertThat(prompt.messages()).extracting(ModelMessage::role)
                .containsExactly(ModelMessage.Role.USER);
        assertThat(prompt.messages().get(0).content()).isEqualTo("user prompt from middleware");
        assertThat(prompt.effectiveUserPrompt())
                .contains("user prompt from middleware")
                .doesNotContain("raw user prompt before middleware");
    }

    @Test
    void marksPromptFallbackWhenModelPromptIsMissing() {
        AtomicReference<ModelPrompt> capturedPrompt = new AtomicReference<>();
        ChatCallModelNode node = new ChatCallModelNode(
                prompt -> {
                    capturedPrompt.set(prompt);
                    return Mono.just(new ModelResponse("<final_answer>fallback ok</final_answer>"));
                },
                new ToolRegistry(List.of()),
                new DeerFlowProperties()
        );

        Map<String, Object> update = node.apply(new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-2",
                AgentGraphStateKeys.THREAD_ID, "thread-2",
                AgentGraphStateKeys.MODE, RunMode.CHAT.name(),
                AgentGraphStateKeys.MODEL_NAME, "fallback-model",
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of()
        ))).join();

        ModelPrompt prompt = capturedPrompt.get();
        assertThat(prompt).isNotNull();
        assertThat(prompt.systemPrompt())
                .contains("You are a helpful assistant.")
                .contains("<tool_call name=\"tool_name\">");
        assertThat(prompt.modelName()).isEqualTo("fallback-model");

        List<Map<String, Object>> messages = (List<Map<String, Object>>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW);
        assertThat(messages).hasSize(1);
        Map<String, Object> metadata = (Map<String, Object>) messages.get(0).get("metadata");
        assertThat(metadata)
                .containsEntry("promptFallback", true)
                .containsEntry("fallbackReason", ChatCallModelNode.PROMPT_FALLBACK_REASON);
    }
}
