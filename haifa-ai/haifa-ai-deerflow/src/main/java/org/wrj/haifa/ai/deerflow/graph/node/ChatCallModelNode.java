package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
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

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            // Fetch state messages using view
            AgentGraphStateView view = AgentGraphStateView.of(state);
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
                    .map(map -> {
                        String roleStr = (String) map.get("role");
                        ModelMessage.Role role = ModelMessage.Role.USER;
                        try {
                            role = ModelMessage.Role.valueOf(roleStr);
                        } catch (Exception ex) {
                            // ignore
                        }
                        String content = (String) map.get("content");
                        return new ModelMessage(role, content);
                    })
                    .toList());
            applyStateUserPrompt(typedHistory, stateUserPrompt);

            // Keep middleware prompt as the source of truth. These are small graph-specific
            // instructions needed by the XML tool-call parser.
            StringBuilder toolDescriptions = new StringBuilder();
            for (AgentTool tool : toolRegistry.tools()) {
                String toolName = tool.name();
                if (toolPolicyService != null && !toolPolicyService.isToolAllowed(toolName, List.of(), view.mode())) {
                    continue;
                }
                toolDescriptions.append("- ").append(toolName).append(": ").append(tool.description()).append("\n");
            }

            String promptReinforcement = "\nIf a user asks for information that can be measured from the local runtime or workspace, and a sandbox execution tool is available, do not claim you lack access. Use the smallest appropriate script, inspect the tool result, then answer from observed output. If the tool is disabled or denied, explain the configuration limitation.";
            StringBuilder graphInstruction = new StringBuilder();
            if (!toolDescriptions.isEmpty()) {
                graphInstruction.append("\n\nAvailable tools:\n").append(toolDescriptions);
            }
            graphInstruction.append("\nWhen you need to use a tool, emit: <tool_call name=\"tool_name\">{\"arg\":\"value\"}</tool_call>\n")
                    .append("Do not write tool calls as prose such as `Tool call: name({...})`; use only the XML tag format above.\n")
                    .append("When you have enough information, provide your final answer starting with <final_answer>.")
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
            ModelPrompt prompt = assembly.prompt();

            long startTime = System.currentTimeMillis();
            ModelResponse response = modelClient.generate(prompt).block();
            long duration = System.currentTimeMillis() - startTime;

            String responseContent = response != null && response.content() != null ? response.content() : "";

            // Emit MODEL_DELTA with full content
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
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "call_model", "status", "completed")));
            return update;
        });
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
