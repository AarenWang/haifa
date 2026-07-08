package org.wrj.haifa.ai.deerflow.todo;

import java.time.Instant;
import java.util.List;

public record TodoSnapshot(
        String threadId,
        String runId,
        int revision,
        String operation,
        List<TodoItem> todos,
        TodoSummary summary,
        Instant updatedAt
) {

    public TodoSnapshot {
        todos = todos == null ? List.of() : List.copyOf(todos);
        summary = summary == null ? TodoSummary.from(todos) : summary;
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static TodoSnapshot of(String threadId, String runId, int revision, String operation, List<TodoItem> todos) {
        return new TodoSnapshot(threadId, runId, revision, operation, todos, TodoSummary.from(todos), Instant.now());
    }
}
