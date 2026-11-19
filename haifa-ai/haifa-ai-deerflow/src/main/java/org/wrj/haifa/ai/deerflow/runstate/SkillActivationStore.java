package org.wrj.haifa.ai.deerflow.runstate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SkillActivationStore {

    private final SkillActivationRepository repository;

    public SkillActivationStore(SkillActivationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SkillActivation activate(String runId, String skillName, String reason, String source, String configJson) {
        SkillActivation act = new SkillActivation();
        act.setActivationId(UUID.randomUUID().toString());
        act.setRunId(runId);
        act.setSkillName(skillName);
        act.setActivationReason(reason);
        act.setSource(source);
        act.setStatus("ACTIVE");
        act.setActivatedAt(Instant.now());
        act.setConfigurationJson(configJson);
        return repository.save(act);
    }

    @Transactional
    public void deactivate(String runId, String skillName) {
        List<SkillActivation> active = repository.findByRunId(runId);
        for (SkillActivation act : active) {
            if (act.getSkillName().equals(skillName) && "ACTIVE".equals(act.getStatus())) {
                act.setStatus("DEACTIVATED");
                act.setDeactivatedAt(Instant.now());
                repository.save(act);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SkillActivation> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public boolean isSkillActive(String runId, String skillName) {
        return repository.findByRunId(runId).stream()
                .anyMatch(act -> act.getSkillName().equals(skillName) && "ACTIVE".equals(act.getStatus()));
    }
}
