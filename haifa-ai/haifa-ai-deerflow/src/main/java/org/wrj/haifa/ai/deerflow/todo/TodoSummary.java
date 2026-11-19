package org.wrj.haifa.ai.deerflow.todo;

import java.util.List;

public record TodoSummary(
        int total,
        int pending,
        int inProgress,
        int completed,
        int cancelled
) {

    public static TodoSummary from(List<TodoItem> todos) {
        if (todos == null || todos.isEmpty()) {
            return new TodoSummary(0, 0, 0, 0, 0);
        }
        return new TodoSummary(
                todos.size(),
                countByStatus(todos, "pending"),
                countByStatus(todos, "in_progress"),
                countByStatus(todos, "completed"),
                countByStatus(todos, "cancelled")
        );
    }

    private static int countByStatus(List<TodoItem> todos, String status) {
        return (int) todos.stream()
                .filter(todo -> status.equalsIgnoreCase(todo.getStatus()))
                .count();
    }
}
