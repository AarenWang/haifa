package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentLoopRunEntity;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;

@Component
public class AgentLoopRunMapper {

    private final JsonMapper jsonMapper;

    public AgentLoopRunMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AgentLoopRunEntity toEntity(String loopId, String runId, String threadId, LoopConfig config) {
        AgentLoopRunEntity entity = new AgentLoopRunEntity();
        entity.setLoopId(loopId);
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setStatus(AgentLoopRunEntity.Status.RUNNING);
        entity.setMaxSteps(config.maxSteps());
        entity.setMaxToolCalls(config.maxToolCalls());
        entity.setTimeoutMs(config.timeoutMs());
        entity.setCurrentStep(0);
        entity.setCreatedAt(java.time.Instant.now());
        entity.setUpdatedAt(java.time.Instant.now());
        return entity;
    }
}
