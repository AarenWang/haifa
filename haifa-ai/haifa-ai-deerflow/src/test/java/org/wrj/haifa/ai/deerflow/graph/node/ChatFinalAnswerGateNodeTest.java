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

    @Test
    void rejectsLocalNumericClaimsAfterFailedMeasurement() {
        ChatFinalAnswerGateNode node = new ChatFinalAnswerGateNode();
        OverAllState state = new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-3",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                "last_assistant_content", "当前能耗排行榜第 1 名是 QQ，能耗评分 34.5。",
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of(
                        "toolName", "run_script",
                        "result", "Script execution denied",
                        "metadata", Map.of("status", "DENIED", "deniedByPolicy", true)))));

        Map<String, Object> update = node.apply(state).join();

        assertThat(update).containsEntry("final_answer_gate_status", "CONTINUE");
        assertThat((List<?>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW)).isNotEmpty();
    }

    @Test
    void rejectsLocalNumericClaimsWhenOnlyChartRenderingSucceeded() {
        ChatFinalAnswerGateNode node = new ChatFinalAnswerGateNode();
        OverAllState state = new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-4",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                "last_assistant_content", "所有数据基于你的设备实时进程快照，Chrome 能耗评分 86.4。",
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of(
                        "toolName", "run_script",
                        "result", "Stdout:\nChart saved to energy_ranking.png\n\nStderr:\n(empty)",
                        "metadata", Map.of(
                                "status", "SUCCESS",
                                "exitCode", 0,
                                "purpose", "Generate energy ranking chart")))));

        Map<String, Object> update = node.apply(state).join();

        assertThat(update).containsEntry("final_answer_gate_status", "CONTINUE");
    }

    @Test
    void acceptsLocalNumericClaimsWithSuccessfulStructuredMeasurement() {
        ChatFinalAnswerGateNode node = new ChatFinalAnswerGateNode();
        OverAllState state = new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-5",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                "last_assistant_content", "当前电量为 91%，预估续航 5 小时 18 分钟。",
                AgentGraphStateKeys.TOOL_RESULTS, List.of(Map.of(
                        "toolName", "run_script",
                        "result", "Stdout:\n{\"RemainingCapacityPercent\":91,\"EstimatedRunTime\":\"05:18\"}\n\nStderr:\n(empty)",
                        "metadata", Map.of(
                                "status", "SUCCESS",
                                "exitCode", 0,
                                "purpose", "Collect Windows battery status")))));

        Map<String, Object> update = node.apply(state).join();

        assertThat(update).containsEntry("final_answer_gate_status", "ACCEPTED");
    }
}
