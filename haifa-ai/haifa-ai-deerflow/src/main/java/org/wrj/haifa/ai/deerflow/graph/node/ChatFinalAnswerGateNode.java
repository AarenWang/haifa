package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.loop.FinalAnswerDecision;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleContext;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;

@Component
public class ChatFinalAnswerGateNode implements AsyncNodeAction {

    private static final int MAX_REPEATED_FINAL_REJECTIONS = 3;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            String answer = state.<String>value("last_assistant_content").orElse("");
            int step = state.<Integer>value("chat_steps").orElse(0);
            AgentGraphStateView view = AgentGraphStateView.of(state);
            List<Map<String, Object>> toolResults = view.listOfMaps(AgentGraphStateKeys.TOOL_RESULTS);
            int totalToolCalls = toolResults.size();

            Map<String, Object> update = new HashMap<>();
            update.put("final_answer_gate_status", "ACCEPTED");
            update.put("accepted_final_answer", answer == null ? "" : answer.trim());
            update.put("accepted_final_metadata", Map.of());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "final_answer_gate", "status", "accepted")));

            GraphChatLifecycleContext context = GraphChatLifecycleRegistry.get(runId).orElse(null);
            boolean maxStepsReached = context != null && context.loopConfig() != null
                    && step >= context.loopConfig().maxSteps();
            if (hasLocalNumericObservationClaim(answer) && !hasSuccessfulLocalDataEvidence(toolResults)) {
                Map<String, Object> evidenceMetadata = Map.of(
                        "unsupportedLocalObservationClaim", true,
                        "requiredEvidence", "successful current-run non-rendering tool result with data payload");
                if (maxStepsReached) {
                    update.put("final_answer_gate_status", "LOCAL_EVIDENCE_MISSING");
                    update.put("accepted_final_answer",
                            "未获得当前运行中可验证的本地观测数据；失败、拒绝或仅完成绘图的工具结果不能作为统计依据。");
                    update.put("accepted_final_metadata", evidenceMetadata);
                    update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of(
                            "node", "final_answer_gate", "status", "local_evidence_missing")));
                    return update;
                }
                update.put("final_answer_gate_status", "CONTINUE");
                update.put(AgentGraphStateKeys.MESSAGE_WINDOW, List.of(systemMessage(threadId, runId,
                        "Do not report local runtime numbers, process names, rankings, or real-time claims. "
                                + "The current run has no successful non-rendering Tool result containing a data payload. "
                                + "A failed/denied Tool provides zero data, and chart/file generation proves only rendering. "
                                + "Retry a smaller safe measurement command or explicitly state that no verifiable data was obtained.",
                        evidenceMetadata)));
                update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of(
                        "node", "final_answer_gate", "status", "local_evidence_rejected")));
                return update;
            }
            if (hasArtifactCompletionClaim(answer) && !hasSuccessfulArtifactDelivery(toolResults)) {
                Map<String, Object> evidenceMetadata = Map.of(
                        "unsupportedCompletionClaim", true,
                        "requiredEvidenceTool", "present_files");
                if (maxStepsReached) {
                    update.put("final_answer_gate_status", "EVIDENCE_MISSING");
                    update.put("accepted_final_answer",
                            "任务未完成：尚未获得可验证的文件生成与交付证据，请检查失败的工具调用后重试。");
                    update.put("accepted_final_metadata", evidenceMetadata);
                    update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of(
                            "node", "final_answer_gate", "status", "evidence_missing")));
                    return update;
                }
                update.put("final_answer_gate_status", "CONTINUE");
                update.put(AgentGraphStateKeys.MESSAGE_WINDOW, List.of(systemMessage(threadId, runId,
                        "Do not claim that a file was generated, saved, delivered, or is downloadable. "
                                + "The run has no successful present_files artifact evidence. Inspect the failed tool result, "
                                + "retry the generic runtime if appropriate, and call present_files only after a non-empty output exists.",
                        evidenceMetadata)));
                update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of(
                        "node", "final_answer_gate", "status", "evidence_rejected")));
                return update;
            }
            if (context == null || context.observer() == null || context.runConfig() == null) {
                return update;
            }

            List<AgentEvent> observerEvents = new ArrayList<>();
            List<String> history = historyFromWindow(view.messageWindow());

            if (!maxStepsReached) {
                int before = history.size();
                boolean shouldContinue = context.observer().shouldContinue(
                        context.runConfig(), answer, observerEvents, context.eventSequence(), step, totalToolCalls, history);
                publishObserverEvents(runId, observerEvents);
                if (shouldContinue) {
                    update.put("final_answer_gate_status", "CONTINUE");
                    update.put(AgentGraphStateKeys.MESSAGE_WINDOW, systemMessages(threadId, runId,
                            newHistoryEntries(history, before), Map.of("observer", "shouldContinue")));
                    update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "final_answer_gate", "status", "continue")));
                    return update;
                }
            }

            FinalAnswerDecision decision = context.observer().onFinalAnswerProposed(
                    context.runConfig(), answer, observerEvents, context.eventSequence(), step, totalToolCalls);
            publishObserverEvents(runId, observerEvents);
            if (decision != null && !decision.accepted() && !maxStepsReached) {
                int repeatedRejections = countFinalAnswerRejections(view.messageWindow(), decision.retryInstruction());
                if (repeatedRejections >= MAX_REPEATED_FINAL_REJECTIONS - 1) {
                    Map<String, Object> metadata = new LinkedHashMap<>(decision.metadata());
                    metadata.put("forcedFinalAnswerAfterRepeatedRejection", true);
                    metadata.put("repeatedFinalAnswerRejections", repeatedRejections + 1);
                    metadata.put("retryInstruction", decision.retryInstruction());
                    update.put("final_answer_gate_status", "FORCED_ACCEPTED");
                    update.put("accepted_final_answer", answer == null ? "" : answer.trim());
                    update.put("accepted_final_metadata", metadata);
                    update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of(
                            "node", "final_answer_gate",
                            "status", "forced_accepted",
                            "repeatedFinalAnswerRejections", repeatedRejections + 1)));
                    return update;
                }
                update.put("final_answer_gate_status", "CONTINUE");
                update.put(AgentGraphStateKeys.MESSAGE_WINDOW, List.of(systemMessage(threadId, runId,
                        decision.retryInstruction(), Map.of("observer", "onFinalAnswerProposed", "finalAnswerRejected", true))));
                update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "final_answer_gate", "status", "rejected")));
                return update;
            }

            if (decision != null && decision.accepted()) {
                update.put("accepted_final_answer", decision.answer());
                update.put("accepted_final_metadata", decision.metadata());
            }
            if (maxStepsReached) {
                context.observer().onMaxStepsReached(context.runConfig(), answer,
                        observerEvents, context.eventSequence(), step, totalToolCalls);
                publishObserverEvents(runId, observerEvents);
            }
            return update;
        }, executor);
    }

    private static void publishObserverEvents(String runId, List<AgentEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AgentEvent event : events) {
            GraphEventRegistry.publish(runId, event);
        }
        events.clear();
    }

    private static boolean hasArtifactCompletionClaim(String answer) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        String lower = answer.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("已成功生成") || lower.contains("成功生成并")
                || lower.contains("已保存至") || lower.contains("已成功交付")
                || lower.contains("可直接下载") || lower.contains("successfully generated")
                || lower.contains("successfully delivered") || lower.contains("saved to")
                || lower.contains("ready for download");
    }

    private static boolean hasSuccessfulArtifactDelivery(List<Map<String, Object>> toolResults) {
        if (toolResults == null) {
            return false;
        }
        for (Map<String, Object> result : toolResults) {
            if (!"present_files".equals(stringValue(result.get("toolName")))) {
                continue;
            }
            Object rawMetadata = result.get("metadata");
            if (!(rawMetadata instanceof Map<?, ?> metadata)) {
                continue;
            }
            boolean success = "SUCCESS".equalsIgnoreCase(stringValue(metadata.get("status")));
            boolean delivered = Boolean.TRUE.equals(metadata.get("artifactDeliverySucceeded"));
            Object ids = metadata.get("presentedArtifactIds");
            boolean hasIds = ids instanceof List<?> list && !list.isEmpty();
            if (success && delivered && hasIds) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLocalNumericObservationClaim(String answer) {
        if (answer == null || answer.isBlank() || !answer.matches("(?s).*\\d.*")) {
            return false;
        }
        String lower = answer.toLowerCase(java.util.Locale.ROOT);
        String[] strongMarkers = {
            "实时", "当前电量", "剩余电量", "预估续航", "本机", "你的设备", "你设备",
            "系统当前", "设备当前", "进程快照", "能耗排行榜", "能耗评分", "功耗排名",
            "cpu 时间", "cpu time", "内存占用", "memory usage", "i/o 活动", "io activity",
            "real-time", "realtime", "current battery", "remaining battery", "your device",
            "process snapshot", "energy ranking", "power ranking"
        };
        for (String marker : strongMarkers) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSuccessfulLocalDataEvidence(List<Map<String, Object>> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return false;
        }
        for (Map<String, Object> result : toolResults) {
            String toolName = stringValue(result.get("toolName")).toLowerCase(java.util.Locale.ROOT);
            if (isRenderingOrMutationTool(toolName)) {
                continue;
            }
            Object rawMetadata = result.get("metadata");
            if (!(rawMetadata instanceof Map<?, ?> metadata)) {
                continue;
            }
            if (!"SUCCESS".equalsIgnoreCase(stringValue(metadata.get("status")))) {
                continue;
            }
            Object exitCode = metadata.get("exitCode");
            if (exitCode instanceof Number number && number.intValue() != 0) {
                continue;
            }
            String purpose = stringValue(metadata.get("purpose")).toLowerCase(java.util.Locale.ROOT);
            if (purpose.contains("chart") || purpose.contains("plot") || purpose.contains("visualization")
                    || purpose.contains("render") || purpose.contains("绘图") || purpose.contains("图表")) {
                continue;
            }
            String payload = observedPayload(stringValue(result.get("result")));
            if (isDataPayload(payload)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRenderingOrMutationTool(String toolName) {
        return toolName.isBlank()
                || toolName.equals("present_files")
                || toolName.equals("write_file")
                || toolName.equals("str_replace")
                || toolName.equals("write_todos")
                || toolName.equals("ask_clarification")
                || toolName.contains("chart")
                || toolName.contains("image")
                || toolName.startsWith("web_");
    }

    private static String observedPayload(String result) {
        if (result == null || result.isBlank()) {
            return "";
        }
        int stdout = result.indexOf("Stdout:");
        if (stdout < 0) {
            return result.trim();
        }
        int start = stdout + "Stdout:".length();
        int stderr = result.indexOf("Stderr:", start);
        return (stderr < 0 ? result.substring(start) : result.substring(start, stderr)).trim();
    }

    private static boolean isDataPayload(String payload) {
        if (payload == null || payload.isBlank() || "(empty)".equalsIgnoreCase(payload.trim())
                || !payload.matches("(?s).*\\d.*")) {
            return false;
        }
        String trimmed = payload.trim();
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("chart saved") || lower.contains("plot saved") || lower.contains("presented artifacts")
                || lower.contains("图表已保存") || lower.contains("图片已保存")) {
            return false;
        }
        boolean structured = trimmed.startsWith("{") || trimmed.startsWith("[")
                || trimmed.contains("\n") || trimmed.contains("\r");
        boolean metricText = lower.contains("battery") || lower.contains("process") || lower.contains("cpu")
                || lower.contains("memory") || lower.contains("energy") || lower.contains("power")
                || lower.contains("电量") || lower.contains("进程") || lower.contains("能耗")
                || lower.contains("功耗") || lower.contains("续航") || lower.contains("内存");
        return structured || metricText;
    }

    private static List<String> historyFromWindow(List<Map<String, Object>> window) {
        List<String> history = new ArrayList<>();
        if (window == null) {
            return history;
        }
        for (Map<String, Object> message : window) {
            String role = stringValue(message.get("role"));
            String content = stringValue(message.get("content"));
            if (content.isBlank()) {
                continue;
            }
            history.add(roleLabel(role) + ": " + content);
        }
        return history;
    }

    private static List<String> newHistoryEntries(List<String> history, int before) {
        if (history == null || before >= history.size()) {
            return List.of();
        }
        return history.subList(Math.max(0, before), history.size());
    }


    private static int countFinalAnswerRejections(List<Map<String, Object>> window, String retryInstruction) {
        if (window == null || retryInstruction == null || retryInstruction.isBlank()) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> message : window) {
            if (!"SYSTEM".equalsIgnoreCase(stringValue(message.get("role")))) {
                continue;
            }
            if (!retryInstruction.equals(stringValue(message.get("content")))) {
                continue;
            }
            Object metadata = message.get("metadata");
            if (metadata instanceof Map<?, ?> metadataMap
                    && Boolean.TRUE.equals(metadataMap.get("finalAnswerRejected"))
                    && "onFinalAnswerProposed".equals(stringValue(metadataMap.get("observer")))) {
                count++;
            }
        }
        return count;
    }
    private static List<Map<String, Object>> systemMessages(String threadId, String runId,
            List<String> entries, Map<String, Object> metadata) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> systemMessage(threadId, runId, stripRolePrefix(entry), metadata))
                .toList();
    }

    private static Map<String, Object> systemMessage(String threadId, String runId, String content,
            Map<String, Object> metadata) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("threadId", threadId);
        message.put("runId", runId);
        message.put("role", ModelMessage.Role.SYSTEM.name());
        message.put("content", content == null ? "" : content);
        message.put("metadata", metadata == null ? Map.of() : metadata);
        message.put("createdAt", java.time.Instant.now().toString());
        return message;
    }

    private static String roleLabel(String role) {
        if ("ASSISTANT".equalsIgnoreCase(role)) return "Assistant";
        if ("TOOL".equalsIgnoreCase(role)) return "Tool result";
        if ("SYSTEM".equalsIgnoreCase(role)) return "System";
        return "User";
    }

    private static String stripRolePrefix(String value) {
        if (value == null) return "";
        if (value.startsWith("System: ")) return value.substring("System: ".length());
        return value;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
