package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatParseModelOutputNode implements AsyncNodeAction {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> pending = AgentGraphStateView.of(state)
                    .listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, pending);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "parse_model_output", "status", "completed")));
            return update;
        });
    }
}
