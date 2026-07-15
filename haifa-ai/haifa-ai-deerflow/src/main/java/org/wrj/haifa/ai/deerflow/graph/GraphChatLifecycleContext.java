package org.wrj.haifa.ai.deerflow.graph;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.skill.Skill;

public record GraphChatLifecycleContext(
        AgentRunConfig runConfig,
        AgentRequest agentRequest,
        LoopConfig loopConfig,
        AgentLoopObserver observer,
        AtomicInteger eventSequence,
        List<Skill> activeSkills,
        List<String> uploadedFileIds
) {
    public GraphChatLifecycleContext {
        activeSkills = activeSkills == null ? List.of() : List.copyOf(activeSkills);
        uploadedFileIds = uploadedFileIds == null ? List.of() : List.copyOf(uploadedFileIds);
    }
}
