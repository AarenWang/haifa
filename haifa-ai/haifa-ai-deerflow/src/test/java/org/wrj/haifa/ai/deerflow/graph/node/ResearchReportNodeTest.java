package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchReportNodeTest {

    @Test
    void reusesExistingArtifactWithoutAppendingItAgain() {
        ResearchReportNode node = new ResearchReportNode(null, null, null, null);

        Map<String, Object> update = node.apply(new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-1",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                AgentGraphStateKeys.FINAL_ANSWER, "existing answer",
                AgentGraphStateKeys.ARTIFACTS, List.of(Map.of(
                        "artifactId", "artifact-1",
                        "filename", "report.md"
                ))
        ))).join();

        assertThat(update)
                .doesNotContainKey(AgentGraphStateKeys.ARTIFACTS)
                .containsEntry(AgentGraphStateKeys.FINAL_ANSWER, "existing answer")
                .containsEntry(AgentGraphStateKeys.RESEARCH_PHASE, "completed");
    }
}
