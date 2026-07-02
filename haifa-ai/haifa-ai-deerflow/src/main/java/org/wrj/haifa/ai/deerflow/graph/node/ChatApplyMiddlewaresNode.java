package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatApplyMiddlewaresNode implements AsyncNodeAction {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> promptMap = state.<Map<String, Object>>value(AgentGraphStateKeys.MODEL_PROMPT).orElse(Map.of());

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MODEL_PROMPT, promptMap);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "apply_prompt_middlewares", "status", "completed")));
            return update;
        });
    }
}
