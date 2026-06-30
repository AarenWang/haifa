package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolCallEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.ToolCallMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.ToolCallRepository;

@Component
public class ToolCallStore {

    private final ToolCallRepository toolCallRepository;
    private final ToolCallMapper toolCallMapper;
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);

    public ToolCallStore(ToolCallRepository toolCallRepository, ToolCallMapper toolCallMapper) {
        this.toolCallRepository = toolCallRepository;
        this.toolCallMapper = toolCallMapper;
    }

    @Transactional
    public ToolCallEntity saveRequested(ToolCall call, String runId, String threadId) {
        ToolCallEntity entity = toolCallMapper.toEntity(call, runId, threadId, sequenceCounter.incrementAndGet());
        toolCallRepository.save(entity);
        return entity;
    }

    @Transactional
    public void saveResult(String toolCallId, ToolCallResult result) {
        toolCallRepository.findById(toolCallId).ifPresent(entity -> {
            toolCallMapper.updateFromResult(entity, result);
            toolCallRepository.save(entity);
        });
    }

    @Transactional(readOnly = true)
    public List<ToolCallEntity> findByRunId(String runId) {
        return toolCallRepository.findByRunIdOrderBySequenceNoAsc(runId);
    }

    @Transactional(readOnly = true)
    public int countByRunId(String runId) {
        return findByRunId(runId).size();
    }
}
