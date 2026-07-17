package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelProtocolState;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.model.ToolCallProtocolState;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
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
                    return Mono.just(new ModelResponse("ok"));
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
                .contains("structured tool-call interface")
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
                    return Mono.just(new ModelResponse("fallback ok"));
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
                .contains("structured tool-call interface");
        assertThat(prompt.modelName()).isEqualTo("fallback-model");

        List<Map<String, Object>> messages = (List<Map<String, Object>>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW);
        assertThat(messages).hasSize(1);
        Map<String, Object> metadata = (Map<String, Object>) messages.get(0).get("metadata");
        assertThat(metadata)
                .containsEntry("promptFallback", true)
                .containsEntry("fallbackReason", ChatCallModelNode.PROMPT_FALLBACK_REASON);
    }

    @Test
    void disclosesConfiguredRunScriptToolFromGraphState() {
        AtomicReference<ModelPrompt> capturedPrompt = new AtomicReference<>();
        AgentTool runScript = tool("run_script");
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        ChatCallModelNode node = new ChatCallModelNode(
                prompt -> {
                    capturedPrompt.set(prompt);
                    return Mono.just(new ModelResponse("ok"));
                },
                new ToolRegistry(List.of(runScript)),
                properties
        );
        ReflectionTestUtils.setField(node, "toolPolicyService", new ToolPolicyService(List.of(), properties));

        node.apply(new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-3",
                AgentGraphStateKeys.THREAD_ID, "thread-3",
                AgentGraphStateKeys.MODE, RunMode.CHAT.name(),
                AgentGraphStateKeys.MODEL_PROMPT, Map.of(
                        "systemPrompt", "middleware prompt",
                        "userPrompt", "user prompt"
                ),
                AgentGraphStateKeys.ACTIVE_SKILLS, List.of(Map.of(
                        "name", "script-skill",
                        "description", "script skill",
                        "source", "test",
                        "allowedTools", List.of("run_script"),
                        "activationHints", List.of()
                )),
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of()
        ))).join();

        assertThat(capturedPrompt.get().systemPrompt())
                .contains("run_script: run_script description");
    }

    @Test
    void sanitizesTruncatedModelToolArgumentsBeforeExecution() {
        ChatCallModelNode node = new ChatCallModelNode(
                prompt -> Mono.just(new ModelResponse("", List.of(
                        new ModelToolCall("call-1", "ask_clarification", "{\"question\":\"unfinished")))),
                new ToolRegistry(List.of()),
                new DeerFlowProperties()
        );

        Map<String, Object> update = node.apply(new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-4",
                AgentGraphStateKeys.THREAD_ID, "thread-4",
                AgentGraphStateKeys.MODE, RunMode.CHAT.name(),
                AgentGraphStateKeys.MODEL_PROMPT, Map.of("systemPrompt", "system", "userPrompt", "user"),
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of()
        ))).join();

        List<Map<String, Object>> pending = (List<Map<String, Object>>) update.get(AgentGraphStateKeys.PENDING_TOOL_CALLS);
        assertThat(pending).singleElement().satisfies(call -> assertThat(call)
                .containsEntry("name", "ask_clarification")
                .containsEntry("arguments", "{}"));
    }

    @Test
    void restoresAndPersistsProtocolStateInGraphMessageWindow() {
        AtomicReference<ModelPrompt> capturedPrompt = new AtomicReference<>();
        ModelProtocolState storedState = new ModelProtocolState(
                ModelProtocolState.CURRENT_SCHEMA_VERSION,
                "google-genai",
                Map.of(),
                List.of(new ToolCallProtocolState(0, "call-1", Map.of("thoughtSignature", "stored")))
        );
        ModelProtocolState responseState = new ModelProtocolState(
                ModelProtocolState.CURRENT_SCHEMA_VERSION,
                "google-genai",
                Map.of(),
                List.of(new ToolCallProtocolState(0, "call-2", Map.of("thoughtSignature", "response")))
        );
        ChatCallModelNode node = new ChatCallModelNode(
                prompt -> {
                    capturedPrompt.set(prompt);
                    return Mono.just(new ModelResponse(
                            "ok", List.of(), List.of(), "STOP", Map.of(), responseState));
                },
                new ToolRegistry(List.of()),
                new DeerFlowProperties()
        );

        Map<String, Object> update = node.apply(new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-protocol-state",
                AgentGraphStateKeys.THREAD_ID, "thread-protocol-state",
                AgentGraphStateKeys.MODE, RunMode.CHAT.name(),
                AgentGraphStateKeys.MODEL_PROMPT, Map.of("systemPrompt", "system", "userPrompt", "user"),
                AgentGraphStateKeys.MESSAGE_WINDOW, List.of(Map.of(
                        "role", ModelMessage.Role.ASSISTANT.name(),
                        "content", "previous tool call",
                        "metadata", Map.of(
                                "protocolState", ModelProtocolState.serializeProtocolState(storedState)
                        )
                ))
        ))).join();

        assertThat(capturedPrompt.get().messages())
                .filteredOn(message -> message.role() == ModelMessage.Role.ASSISTANT)
                .singleElement()
                .extracting(ModelMessage::protocolState)
                .isEqualTo(storedState);
        List<Map<String, Object>> messages = (List<Map<String, Object>>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW);
        Map<String, Object> metadata = (Map<String, Object>) messages.get(0).get("metadata");
        assertThat(ModelProtocolState.deserializeProtocolState(metadata.get("protocolState")))
                .isEqualTo(responseState);
    }

    private static AgentTool tool(String name) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name + " description";
            }

            @Override
            public boolean supports(String userMessage) {
                return true;
            }

            @Override
            public ToolResult execute(ToolRequest request) {
                return ToolResult.of(name, "ok");
            }
        };
    }
}


