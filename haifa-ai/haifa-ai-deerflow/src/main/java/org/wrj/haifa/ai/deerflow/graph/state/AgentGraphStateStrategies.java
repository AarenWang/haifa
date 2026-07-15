package org.wrj.haifa.ai.deerflow.graph.state;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;

public final class AgentGraphStateStrategies {

    private AgentGraphStateStrategies() {
    }

    public static KeyStrategyFactory keyStrategyFactory() {
        return KeyStrategy.builder()
                .defaultStrategy(KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RUN_ID, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.THREAD_ID, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.MODE, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.USER_ID, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.USER_MESSAGE, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.MODEL_NAME, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.ACTIVE_SKILLS, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.UPLOADED_FILE_IDS, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.REQUEST_METADATA, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.MESSAGE_WINDOW, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.RUN_PROMPT_BASE, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RUN_PREPARED, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.PROMPT_REVISION, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.MODEL_PROMPT, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.MODEL_STEPS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.TOOL_CALLS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.TOOL_RESULTS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.PENDING_TOOL_CALLS, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_PLAN_REF, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_PHASE, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_STEPS, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.QUALITY_GATE_PASSED, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_GAPS, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.REPLAN_COUNT, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.CITATION_VERIFICATION, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.EMITTED_EVIDENCE_IDS, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.ARTIFACTS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.ERRORS, KeyStrategy.APPEND)
                .addStrategy(AgentGraphStateKeys.TODOS, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.SUBAGENTS, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.CLARIFICATION, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.SANDBOX, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.FINAL_ANSWER, KeyStrategy.REPLACE)
                .addStrategy(AgentGraphStateKeys.USAGE, KeyStrategy.MERGE)
                .addStrategy(AgentGraphStateKeys.RESEARCH_OPTIONS, KeyStrategy.REPLACE)
                .build();
    }
}
