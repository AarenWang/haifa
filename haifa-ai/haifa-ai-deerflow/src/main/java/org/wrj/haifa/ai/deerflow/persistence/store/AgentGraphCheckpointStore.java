package org.wrj.haifa.ai.deerflow.persistence.store;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.AgentGraphCheckpointRecord;
import org.wrj.haifa.ai.deerflow.persistence.mapper.AgentGraphCheckpointMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.AgentGraphCheckpointRepository;

import java.util.List;

@Component
public class AgentGraphCheckpointStore {

    private final AgentGraphCheckpointRepository repository;
    private final AgentGraphCheckpointMapper mapper;

    public AgentGraphCheckpointStore(AgentGraphCheckpointRepository repository, AgentGraphCheckpointMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public void saveAll(List<AgentGraphCheckpointRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        repository.saveAll(records.stream().map(mapper::toEntity).toList());
    }

    @Transactional(readOnly = true)
    public List<AgentGraphCheckpointRecord> findByRunId(String runId) {
        return repository.findByRunIdOrderByCreatedAtAsc(runId).stream()
                .map(mapper::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentGraphCheckpointRecord> findByThreadId(String threadId) {
        return repository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(mapper::toRecord)
                .toList();
    }
}
