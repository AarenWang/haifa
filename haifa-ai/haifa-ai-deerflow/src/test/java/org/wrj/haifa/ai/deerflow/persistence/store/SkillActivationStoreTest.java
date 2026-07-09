package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.runstate.SkillActivation;
import org.wrj.haifa.ai.deerflow.runstate.SkillActivationStore;

@SpringBootTest
@ActiveProfiles("test")
class SkillActivationStoreTest {

    @Autowired
    private SkillActivationStore skillActivationStore;

    @Test
    void testActivateAndDeactivate() {
        String runId = "test-run-1";
        String skillName = "deep-research";

        SkillActivation active = skillActivationStore.activate(runId, skillName, "Explicit choice", "user_explicit", "{}");
        assertThat(active.getActivationId()).isNotNull();
        assertThat(active.getRunId()).isEqualTo(runId);
        assertThat(active.getSkillName()).isEqualTo(skillName);
        assertThat(active.getStatus()).isEqualTo("ACTIVE");

        List<SkillActivation> list = skillActivationStore.findByRunId(runId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo("ACTIVE");

        skillActivationStore.deactivate(runId, skillName);

        list = skillActivationStore.findByRunId(runId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo("DEACTIVATED");
        assertThat(list.get(0).getDeactivatedAt()).isNotNull();
    }
}
