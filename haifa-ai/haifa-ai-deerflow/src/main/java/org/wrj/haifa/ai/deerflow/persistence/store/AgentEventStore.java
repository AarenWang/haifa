package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentEventEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.AgentEventMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.AgentEventRepository;

@Component
public class AgentEventStore {

    private final AgentEventRepository agentEventRepository;
    private final AgentEventMapper agentEventMapper;
    private final AtomicInteger sequenceCounter;

    public AgentEventStore(AgentEventRepository agentEventRepository, AgentEventMapper agentEventMapper) {
        this.agentEventRepository = agentEventRepository;
        this.agentEventMapper = agentEventMapper;
        Integer maxSeq = agentEventRepository.findAll().stream()
                .map(AgentEventEntity::getSequenceNo)
                .filter(n -> n != null)
                .max(Integer::compare)
                .orElse(0);
        this.sequenceCounter = new AtomicInteger(maxSeq);
    }

    @Transactional
    public void save(AgentEvent event) {
        AgentEventEntity entity = agentEventMapper.toEntity(event);
        entity.setSequenceNo(sequenceCounter.incrementAndGet());
        agentEventRepository.save(entity);
    }

    @Transactional
    public void saveAll(List<AgentEvent> events) {
        for (AgentEvent event : events) {
            AgentEventEntity entity = agentEventMapper.toEntity(event);
            entity.setSequenceNo(sequenceCounter.incrementAndGet());
            agentEventRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public List<AgentEvent> findByRunId(String runId) {
        return agentEventRepository.findByRunIdOrderBySequenceNoAsc(runId).stream()
                .map(agentEventMapper::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentEvent> findByThreadId(String threadId) {
        return agentEventRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(agentEventMapper::toRecord)
                .toList();
    }
}
