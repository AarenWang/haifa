package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoSnapshot;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;

/**
 * Default implementation of AgentLoopObserver with todo checklist verification.
 * Tool output compression is handled centrally by {@link AgentLoop}.
 */
public class DefaultAgentLoopObserver implements AgentLoopObserver {

    protected final TodoStore todoStore;

    public DefaultAgentLoopObserver(TodoStore todoStore) {
        this.todoStore = todoStore;
    }

    @Override
    public String onToolCompleted(AgentRunConfig runConfig, ToolCall toolCall, ToolCallResult toolResult,
            List<AgentEvent> events, AtomicInteger seq, List<String> history) {
        // Tool output compression is now handled by AgentLoop before event emission.
        // Observers only handle source/evidence processing and observations.
        return null;
    }

    @Override
    public void onStepCompleted(AgentRunConfig runConfig, List<AgentEvent> events, AtomicInteger seq, int step) {
    }

    @Override
    public boolean shouldContinue(AgentRunConfig runConfig, String responseContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls, List<String> history) {
        return false;
    }

    @Override
    public FinalAnswerDecision onFinalAnswerProposed(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        if (totalToolCalls == 0 && claimsCreatedDownloadableFile(rawAnswer)) {
            return FinalAnswerDecision.reject(
                    "Do not claim that a downloadable file was created unless a tool actually wrote and registered it. "
                            + "Call `write_file` with a path under `outputs/` first, then provide the artifact download link.",
                    Map.of("reason", "artifact_claim_without_tool", "toolCount", 0));
        }

        if (todoStore == null) {
            return FinalAnswerDecision.accept(rawAnswer, Map.of());
        }

        List<TodoItem> todos = todoStore.listTodos(runConfig.threadId(), runConfig.runId());
        TodoSnapshot snapshot = todoStore.snapshot(runConfig.threadId(), runConfig.runId());
        if (todos.isEmpty() && runConfig.mode() == org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH) {
            return FinalAnswerDecision.reject(
                    "Do not finish yet. This research run has no TodoList. Call `write_todos` first to create a complete plan, then continue the work.",
                    Map.of("reason", "missing_todos", "todoCount", 0, "snapshot", snapshot));
        }

        List<TodoItem> incomplete = todos.stream()
                .filter(t -> !"completed".equalsIgnoreCase(t.getStatus()))
                .toList();
        if (!incomplete.isEmpty()) {
            String instruction = "Do not finish yet. The following todos are still incomplete: "
                    + String.join("; ", incomplete.stream()
                            .map(todo -> "[" + todo.getStatus() + "] " + todo.getContent())
                            .toList())
                    + ". Continue working through the todo list and call `write_todos` after each status change.";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("reason", "incomplete_todos");
            metadata.put("incompleteTodos", incomplete);
            metadata.put("incompleteTodoIds", incomplete.stream().map(TodoItem::getId).toList());
            metadata.put("incompleteCount", incomplete.size());
            metadata.put("snapshot", snapshot);
            return FinalAnswerDecision.reject(instruction, metadata);
        }

        return FinalAnswerDecision.accept(rawAnswer, Map.of("todoCount", todos.size()));
    }

    private static boolean claimsCreatedDownloadableFile(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return false;
        }
        String normalized = rawAnswer.toLowerCase();
        boolean mentionsFileType = normalized.contains(".eml")
                || normalized.contains(".pdf")
                || normalized.contains(".docx")
                || normalized.contains(".xlsx")
                || normalized.contains(".pptx")
                || normalized.contains(".zip")
                || normalized.contains(".md");
        boolean claimsCreated = normalized.contains("已保存")
                || normalized.contains("已生成")
                || normalized.contains("供下载")
                || normalized.contains("download")
                || normalized.contains("saved to")
                || normalized.contains("generated");
        return mentionsFileType && claimsCreated;
    }

    @Override
    public FinalAnswerResult onFinalAnswerAccepted(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        return new FinalAnswerResult(rawAnswer, Map.of());
    }

    @Override
    public void onMaxStepsReached(AgentRunConfig runConfig, String lastModelContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
    }
}
