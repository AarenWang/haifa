package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.agent.loop.PromptAssembler;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatCallModelNode implements AsyncNodeAction {

    static final String PROMPT_FALLBACK_REASON = "MODEL_PROMPT_MISSING_OR_EMPTY";
    private static final String DEFAULT_FALLBACK_SYSTEM_PROMPT = "You are a helpful assistant.";

    private final AgentModelClient modelClient;
    private final ToolRegistry toolRegistry;
    private final DeerFlowProperties properties;
    private final PromptAssembler promptAssembler;

    @Autowired(required = false)
    private ToolPolicyService toolPolicyService;

    public ChatCallModelNode(AgentModelClient modelClient,
                             ToolRegistry toolRegistry,
                             DeerFlowProperties properties) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.promptAssembler = new PromptAssembler();
    }

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            // Fetch state messages using view
            AgentGraphStateView view = AgentGraphStateView.of(state);
            var activeSkills = view.activeSkills();
            Map<String, Object> promptMap = view.map(AgentGraphStateKeys.MODEL_PROMPT);
            String stateSystemPrompt = stringValue(promptMap.get("systemPrompt"));
            String stateUserPrompt = stringValue(promptMap.get("userPrompt"));
            String modelName = firstNonBlank(
                    stringValue(promptMap.get("modelName")),
                    state.<String>value(AgentGraphStateKeys.MODEL_NAME).orElse(null)
            );
            boolean promptFallback = stateSystemPrompt.isBlank();
            String systemPromptBase = promptFallback ? DEFAULT_FALLBACK_SYSTEM_PROMPT : stateSystemPrompt;

            List<Map<String, Object>> windowMaps = view.messageWindow();
            List<ModelMessage> typedHistory = new ArrayList<>(windowMaps.stream()
                    .map(ChatCallModelNode::toModelMessage)
                    .toList());
            applyStateUserPrompt(typedHistory, stateUserPrompt);
            StringBuilder toolDescriptions = new StringBuilder();
            List<ModelToolDefinition> toolDefinitions = new ArrayList<>();
            for (AgentTool tool : toolRegistry.tools()) {
                String toolName = tool.name();
                if (toolPolicyService != null && !toolPolicyService.evaluateTool(toolName, activeSkills, view.mode()).allowed()) {
                    continue;
                }
                toolDescriptions.append("- ").append(toolName).append(": ").append(tool.description()).append("\n");
                toolDefinitions.add(new ModelToolDefinition(toolName, tool.description(), tool.inputSchema()));
            }

            String promptReinforcement = "\nIf a user asks for information that can be measured from the local runtime or workspace, and a sandbox execution tool is available, do not claim you lack access. Use the smallest appropriate script, inspect the tool result, then answer from observed output. If the tool is disabled or denied, explain the configuration limitation.";
            StringBuilder graphInstruction = new StringBuilder();
            if (!toolDescriptions.isEmpty()) {
                graphInstruction.append("\n\nAvailable tools:\n").append(toolDescriptions);
            }
            graphInstruction.append("\nWhen external information or actions are needed, call the available tools through the model provider's structured tool-call interface.\n")
                    .append("Do not write tool calls manually in XML, JSON code blocks, markdown, or prose.\n")
                    .append("When no further tool call is needed, respond with the final answer directly in normal assistant text.")
                    .append(promptReinforcement);
            String fullSystemPrompt = systemPromptBase + graphInstruction;

            // Emit MODEL_STARTED
            int stepNum = state.<Integer>value("chat_steps").orElse(0) + 1;
            int maxSteps = properties.getMaxIterations();
            Map<String, Object> modelStartedMetadata = new LinkedHashMap<>();
            modelStartedMetadata.put("step", stepNum);
            modelStartedMetadata.put("maxSteps", maxSteps);
            if (promptFallback) {
                modelStartedMetadata.put("promptFallback", true);
                modelStartedMetadata.put("fallbackReason", PROMPT_FALLBACK_REASON);
            }
            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.MODEL_STARTED,
                    "Model step " + stepNum + "/" + maxSteps,
                    modelStartedMetadata
            ));

            PromptAssembler.Result assembly = promptAssembler.assemble(fullSystemPrompt, modelName, typedHistory);
            ModelPrompt prompt = assembly.prompt().withToolDefinitions(toolDefinitions);

            long startTime = System.currentTimeMillis();
            StringBuilder fullContent = new StringBuilder();
            List<ModelToolCall> accumulatedToolCalls = new ArrayList<>();
            java.util.concurrent.atomic.AtomicReference<String> finishReasonRef = new java.util.concurrent.atomic.AtomicReference<>();

            reactor.core.publisher.Flux<ModelResponse> stream = modelClient.streamGenerate(prompt);
            if (stream == null) {
                stream = modelClient.generate(prompt).flux();
            }
            stream
                    .doOnNext(response -> {
                        String chunk = response.content();
                        if (chunk != null && !chunk.isEmpty()) {
                            fullContent.append(chunk);
                            GraphEventRegistry.publish(runId, AgentEvent.of(
                                    UUID.randomUUID().toString(),
                                    runId,
                                    threadId,
                                    AgentEventType.MODEL_DELTA,
                                    chunk,
                                    Map.of("step", stepNum)
                            ));
                        }
                        if (response.toolCalls() != null && !response.toolCalls().isEmpty()) {
                            accumulatedToolCalls.addAll(response.toolCalls());
                        }
                        if (response.finishReason() != null) {
                            finishReasonRef.set(response.finishReason());
                        }
                    })
                    .then()
                    .block(java.time.Duration.ofMillis(properties.getModelTimeout()));

            long duration = System.currentTimeMillis() - startTime;
            String responseContent = fullContent.toString();
            List<Map<String, Object>> structuredToolCalls = serializeToolCalls(accumulatedToolCalls);

            // Emit final MODEL_DELTA containing the complete text for content synchronization
            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.MODEL_DELTA,
                    responseContent,
                    Map.of("step", stepNum, "modelDurationMs", duration)
            ));

            // APPEND strategy expects only newly produced messages.
            Map<String, Object> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("messageId", UUID.randomUUID().toString());
            assistantMsg.put("threadId", threadId);
            assistantMsg.put("runId", runId);
            assistantMsg.put("role", ModelMessage.Role.ASSISTANT.name());
            assistantMsg.put("content", responseContent);
            Map<String, Object> assistantMetadata = new LinkedHashMap<>();
            assistantMetadata.put("step", stepNum);
            assistantMetadata.put("modelDurationMs", duration);
            if (!structuredToolCalls.isEmpty()) {
                assistantMetadata.put("tool_calls", structuredToolCalls);
                assistantMetadata.put("persistAssistantToolCalls", true);
                assistantMsg.put("toolCalls", structuredToolCalls);
            }
            if (promptFallback) {
                assistantMetadata.put("promptFallback", true);
                assistantMetadata.put("fallbackReason", PROMPT_FALLBACK_REASON);
            }
            assistantMsg.put("metadata", assistantMetadata);
            assistantMsg.put("createdAt", java.time.Instant.now().toString());

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MESSAGE_WINDOW, List.of(assistantMsg));
            update.put("chat_steps", stepNum);
            update.put("last_assistant_content", responseContent);
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, structuredToolCalls);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "call_model", "status", "completed")));
            return update;
        }, executor);
    }

    private static ModelMessage toModelMessage(Map<String, Object> map) {
        String roleStr = stringValue(map.get("role"));
        ModelMessage.Role role = ModelMessage.Role.USER;
        try {
            role = ModelMessage.Role.valueOf(roleStr);
        } catch (Exception ex) {
            // Keep the default role for malformed persisted records.
        }
        String content = stringValue(map.get("content"));
        String toolCallId = stringValue(map.get("toolCallId"));
        String name = stringValue(map.get("name"));
        Map<String, Object> metadata = readMetadata(map.get("metadata"));
        if (toolCallId.isBlank()) {
            toolCallId = stringValue(metadata.get("toolCallId"));
        }
        if (toolCallId.isBlank()) {
            toolCallId = stringValue(metadata.get("tool_call_id"));
        }
        if (name.isBlank()) {
            name = stringValue(metadata.get("name"));
        }
        if (name.isBlank()) {
            name = stringValue(metadata.get("toolName"));
        }
        List<ModelToolCall> toolCalls = readToolCalls(map.get("toolCalls"));
        if (toolCalls.isEmpty()) {
            toolCalls = readToolCalls(metadata.get("tool_calls"));
        }
        return new ModelMessage(role, content, toolCalls, toolCallId.isBlank() ? null : toolCallId,
                name.isBlank() ? null : name, metadata);
    }

    private static Map<String, Object> readMetadata(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        raw.forEach((key, item) -> {
            if (key instanceof String name) {
                metadata.put(name, item);
            }
        });
        return metadata;
    }

    private static List<ModelToolCall> readToolCalls(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<ModelToolCall> calls = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                calls.add(new ModelToolCall(
                        stringValue(map.get("id")),
                        stringValue(map.get("name")),
                        firstNonBlank(stringValue(map.get("arguments")), "{}"),
                        firstNonBlank(stringValue(map.get("type")), "tool_call")));
            }
        }
        return calls;
    }

    private static List<Map<String, Object>> serializeToolCalls(List<ModelToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        return toolCalls.stream()
                .map(toolCall -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", toolCall.id() == null ? "" : toolCall.id());
                    item.put("name", toolCall.name() == null ? "" : toolCall.name());
                    item.put("arguments", toolCall.arguments() == null ? "{}" : toolCall.arguments());
                    item.put("type", toolCall.type() == null ? "tool_call" : toolCall.type());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private static String stringValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static void applyStateUserPrompt(List<ModelMessage> typedHistory, String stateUserPrompt) {
        if (stateUserPrompt == null || stateUserPrompt.isBlank()) {
            return;
        }
        for (int i = typedHistory.size() - 1; i >= 0; i--) {
            ModelMessage message = typedHistory.get(i);
            if (message.role() == ModelMessage.Role.USER) {
                if (!stateUserPrompt.equals(message.content())) {
                    typedHistory.set(i, new ModelMessage(ModelMessage.Role.USER, stateUserPrompt));
                }
                return;
            }
        }
        typedHistory.add(new ModelMessage(ModelMessage.Role.USER, stateUserPrompt));
    }
}
