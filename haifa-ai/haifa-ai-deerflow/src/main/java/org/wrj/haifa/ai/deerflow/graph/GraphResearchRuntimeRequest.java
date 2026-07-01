package org.wrj.haifa.ai.deerflow.graph;

import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public record GraphResearchRuntimeRequest(
        AgentLoop agentLoop,
        LoopConfig loopConfig,
        AgentRunConfig runConfig,
        AgentRequest agentRequest,
        String systemPrompt,
        String userPrompt,
        AtomicInteger eventSequence,
        ToolPolicyService toolPolicyService,
        List<Skill> activeSkills,
        List<String> uploadedFileIds,
        List<MessageRecord> threadHistory
) {
}
