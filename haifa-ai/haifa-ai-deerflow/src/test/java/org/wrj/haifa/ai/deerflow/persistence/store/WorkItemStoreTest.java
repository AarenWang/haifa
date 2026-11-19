package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.work.WorkItem;
import org.wrj.haifa.ai.deerflow.work.WorkItemStore;

@SpringBootTest
@ActiveProfiles("test")
class WorkItemStoreTest {

    @Autowired
    private WorkItemStore workItemStore;

    @Test
    void testWorkItemLifecycle() {
        String runId = "run-wi-1";
        String threadId = "thread-wi-1";

        WorkItem item = workItemStore.create(runId, threadId, null, "research_question", "Question 1", "Goal 1", "high", "parent_agent");
        assertThat(item.getWorkItemId()).isNotNull();
        assertThat(item.getStatus()).isEqualTo("proposed");

        workItemStore.updateStatus(item.getWorkItemId(), "completed", null, null);

        List<WorkItem> list = workItemStore.findByRunId(runId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo("completed");
        assertThat(list.get(0).getCompletedAt()).isNotNull();

        workItemStore.addEvidenceId(item.getWorkItemId(), "ev-1");
        WorkItem updated = workItemStore.addArtifactId(item.getWorkItemId(), "art-1");

        assertThat(updated.getEvidenceIds()).contains("ev-1");
        assertThat(updated.getArtifactIds()).contains("art-1");
    }
}
