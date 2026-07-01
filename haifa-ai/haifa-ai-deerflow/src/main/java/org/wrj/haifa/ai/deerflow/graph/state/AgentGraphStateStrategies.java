package org.wrj.haifa.ai.deerflow.graph.state;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;

public final class AgentGraphStateStrategies {

    private AgentGraphStateStrategies() {
    }

    public static KeyStrategyFactory keyStrategyFactory() {
        return KeyStrategy.builder()
                .defaultStrategy(KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.MESSAGE_WINDOW, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.MODEL_STEPS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.TOOL_CALLS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.TOOL_RESULTS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.ARTIFACTS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.ERRORS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.TODOS, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.SUBAGENTS, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.CLARIFICATION, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.SANDBOX, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.USAGE, KeyStrategy.MERGE)
                .build();
    }
}
