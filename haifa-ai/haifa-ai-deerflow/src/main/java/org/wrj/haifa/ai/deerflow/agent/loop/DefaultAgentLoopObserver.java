package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
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
        if (todoStore == null) {
            return false;
        }
        List<TodoItem> todos = todoStore.listTodos(runConfig.threadId(), runConfig.runId());
        List<TodoItem> incomplete = todos.stream()
                .filter(t -> !"completed".equalsIgnoreCase(t.getStatus()))
                .toList();
        if (!incomplete.isEmpty()) {
            String instruction = "Do not finish yet. The following tasks are still incomplete: "
                    + String.join("; ", incomplete.stream().map(TodoItem::getContent).toList())
                    + ". Continue working through the todo list. Mark each task as completed immediately after finishing it.";
            history.add("System: " + instruction);
            events.add(AgentEvent.of(Integer.toString(seq.incrementAndGet()), runConfig.runId(), runConfig.threadId(),
                    AgentEventType.TODO_INCOMPLETE, "Incomplete todos prevent final answer",
                    Map.of("incompleteTodos", incomplete.stream().map(TodoItem::getId).toList())));
            return true;
        }
        return false;
    }

    @Override
    public FinalAnswerResult onFinalAnswerAccepted(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        String finalAnswer = rawAnswer;
        if (todoStore != null) {
            List<TodoItem> todos = todoStore.listTodos(runConfig.threadId(), runConfig.runId());
            List<TodoItem> incomplete = todos.stream()
                    .filter(t -> !"completed".equalsIgnoreCase(t.getStatus()))
                    .toList();
            if (!incomplete.isEmpty()) {
                List<String> incompleteDesc = incomplete.stream().map(TodoItem::getContent).toList();
                events.add(AgentEvent.of(Integer.toString(seq.incrementAndGet()), runConfig.runId(), runConfig.threadId(),
                        AgentEventType.TODO_INCOMPLETE, "Some tasks were left incomplete: " + String.join(", ", incompleteDesc),
                        Map.of("incompleteTodos", incomplete.stream().map(TodoItem::getId).toList())));
                
                StringBuilder sb = new StringBuilder(rawAnswer);
                sb.append("\n\n**Limitations**: The following planned tasks were not completed: ");
                for (int i = 0; i < incomplete.size(); i++) {
                    sb.append(incomplete.get(i).getContent());
                    if (i < incomplete.size() - 1) {
                        sb.append("; ");
                    }
                }
                sb.append(".");
                finalAnswer = sb.toString();
            }
        }
        return new FinalAnswerResult(finalAnswer, Map.of());
    }

    @Override
    public void onMaxStepsReached(AgentRunConfig runConfig, String lastModelContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
    }
}
