package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallParser;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatParseModelOutputNode implements AsyncNodeAction {

    private final ToolCallParser parser;

    public ChatParseModelOutputNode() {
        this.parser = new ToolCallParser();
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String content = state.<String>value("last_assistant_content").orElse("");

            List<ToolCallParser.ParsedToolCall> parsed = parser.parse(content);
            List<Map<String, Object>> pending = new ArrayList<>();
            for (ToolCallParser.ParsedToolCall call : parsed) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", "tc-" + UUID.randomUUID().toString().substring(0, 8));
                map.put("name", call.toolName());
                map.put("arguments", call.arguments());
                pending.add(map);
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, pending);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "parse_model_output", "status", "completed")));
            return update;
        });
    }
}
