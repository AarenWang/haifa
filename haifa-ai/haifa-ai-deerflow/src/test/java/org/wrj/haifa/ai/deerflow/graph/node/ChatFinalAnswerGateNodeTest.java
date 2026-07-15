package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;

import static org.assertj.core.api.Assertions.assertThat;

class ChatFinalAnswerGateNodeTest {

    @Test
    void rejectsFileSuccessClaimWithoutPresentFilesEvidence() {
        ChatFinalAnswerGateNode node = new ChatFinalAnswerGateNode();
        OverAllState state = new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-1",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                "last_assistant_content", "图片已成功生成并交付，可直接下载。",
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of(
                        "toolName", "bash",
                        "result", "Exit code: 1",
                        "metadata", Map.of("status", "FAILED", "exitCode", 1)))));

        Map<String, Object> update = node.apply(state).join();

        assertThat(update).containsEntry("final_answer_gate_status", "CONTINUE");
        assertThat((List<?>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW)).isNotEmpty();
    }

    @Test
    void acceptsFileSuccessClaimWithRegisteredDeliveryEvidence() {
        ChatFinalAnswerGateNode node = new ChatFinalAnswerGateNode();
        OverAllState state = new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-2",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                "last_assistant_content", "图片已成功生成并交付，可直接下载。",
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of(
                        "toolName", "present_files",
                        "result", "Presented artifacts",
                        "metadata", Map.of(
                                "status", "SUCCESS",
                                "artifactDeliverySucceeded", true,
                                "presentedArtifactIds", List.of("artifact-1"))))));

        Map<String, Object> update = node.apply(state).join();

        assertThat(update).containsEntry("final_answer_gate_status", "ACCEPTED");
    }
}
