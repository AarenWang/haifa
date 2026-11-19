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

    @Transactional
    public void saveGraphStep(int stepIndex, String prompt, String response, long startedAt, long durationMs,
            org.wrj.haifa.ai.deerflow.model.cache.ModelUsage usage,
            org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint fingerprint,
            org.wrj.haifa.ai.deerflow.model.cache.PromptCacheEligibility eligibility,
            String runId, String threadId) {
        save(new ModelStep(stepIndex, prompt, response, List.of(), startedAt, durationMs, usage, fingerprint,
                org.wrj.haifa.ai.deerflow.model.cache.ModelCallPurpose.AGENT_STEP, eligibility), runId, threadId);
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
