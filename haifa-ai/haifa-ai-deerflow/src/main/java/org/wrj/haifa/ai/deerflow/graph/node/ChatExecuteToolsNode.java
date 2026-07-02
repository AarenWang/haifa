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
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolExecutionStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatExecuteToolsNode implements AsyncNodeAction {

    private final ToolRegistry toolRegistry;
    private final DeerFlowProperties properties;

    @Autowired(required = false)
    private ToolCallStore toolCallStore;

    @Autowired(required = false)
    private ToolExecutionStore toolExecutionStore;

    public ChatExecuteToolsNode(ToolRegistry toolRegistry, DeerFlowProperties properties) {
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            AgentGraphStateView view = AgentGraphStateView.of(state);
            List<Map<String, Object>> pending = view.listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);
            List<Map<String, Object>> window = new ArrayList<>(view.messageWindow());
            List<Map<String, Object>> toolResultsList = new ArrayList<>(view.toolResults());

            for (Map<String, Object> call : pending) {
                String callId = (String) call.get("id");
                String name = (String) call.get("name");
                String arguments = (String) call.get("arguments");

                GraphEventRegistry.publish(runId, AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.TOOL_STARTED,
                        "Executing tool " + name,
                        Map.of("toolCallId", callId, "toolName", name)
                ));

                long startTime = System.currentTimeMillis();
                ToolResult result;
                try {
                    AgentTool tool = toolRegistry.tools().stream()
                            .filter(t -> t.name().equals(name))
                            .findFirst()
                            .orElse(null);

                    if (tool == null) {
                        result = ToolResult.of(name, "Error: tool not found: " + name);
                    } else {
                        Path workspaceRoot = Path.of(properties.getWorkspaceRoot() != null ? properties.getWorkspaceRoot() : ".");
                        ToolRequest toolRequest = new ToolRequest(
                                arguments,
                                workspaceRoot,
                                List.of(),
                                threadId,
                                runId,
                                view.mode(),
                                List.of()
                        );
                        result = tool.execute(toolRequest);
                    }
                } catch (Exception ex) {
                    result = ToolResult.of(name, "Error executing tool: " + ex.getMessage());
                }
                long duration = System.currentTimeMillis() - startTime;

                GraphEventRegistry.publish(runId, AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.TOOL_COMPLETED,
                        result.content(),
                        Map.of("toolCallId", callId, "toolName", name, "status", "SUCCESS", "durationMs", duration)
                ));

                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("messageId", UUID.randomUUID().toString());
                toolMsg.put("threadId", threadId);
                toolMsg.put("runId", runId);
                toolMsg.put("role", ModelMessage.Role.TOOL.name());
                toolMsg.put("content", result.content());
                toolMsg.put("name", name);
                toolMsg.put("toolCallId", callId);
                toolMsg.put("metadata", Map.of("durationMs", duration));
                toolMsg.put("createdAt", java.time.Instant.now().toString());
                window.add(toolMsg);

                Map<String, Object> resMap = new LinkedHashMap<>();
                resMap.put("toolName", name);
                resMap.put("result", result.content());
                resMap.put("metadata", result.metadata());
                toolResultsList.add(resMap);
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MESSAGE_WINDOW, window);
            update.put(AgentGraphStateKeys.TOOL_RESULTS, toolResultsList);
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "execute_tools", "status", "completed")));
            return update;
        });
    }
}
