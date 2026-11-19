package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ResearchPlannerAgentNode implements AsyncNodeAction {

    private final ResearchPlanningNode delegate;

    public ResearchPlannerAgentNode(ResearchPlanningNode delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return delegate.apply(state);
    }
}
