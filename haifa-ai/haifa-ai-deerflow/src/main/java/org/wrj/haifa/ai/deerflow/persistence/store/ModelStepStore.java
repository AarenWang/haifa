package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.agent.loop.ModelStep;
import org.wrj.haifa.ai.deerflow.persistence.entity.ModelStepEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.ModelStepMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.ModelStepRepository;

@Component
public class ModelStepStore {

    private final ModelStepRepository modelStepRepository;
    private final ModelStepMapper modelStepMapper;

    public ModelStepStore(ModelStepRepository modelStepRepository, ModelStepMapper modelStepMapper) {
        this.modelStepRepository = modelStepRepository;
        this.modelStepMapper = modelStepMapper;
    }

    @Transactional
    public void save(ModelStep step, String runId, String threadId) {
        ModelStepEntity entity = modelStepMapper.toEntity(step, runId, threadId);
        modelStepRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ModelStepEntity> findByRunId(String runId) {
        return modelStepRepository.findByRunIdOrderByStepIndexAsc(runId);
    }

    @Transactional(readOnly = true)
    public int countByRunId(String runId) {
        return findByRunId(runId).size();
    }
}
