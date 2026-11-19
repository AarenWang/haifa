package org.wrj.haifa.ai.deerflow.todo;

import java.util.List;

/**
 * Store to persist and manage TodoLists by threadId.
 */
public interface TodoStore {

    /**
     * Lists todos associated with a thread/run ID.
     */
    List<TodoItem> listTodos(String threadId, String runId);

    default List<TodoItem> listTodos(String threadId) {
        return listTodos(threadId, null);
    }

    /**
     * Saves or replaces the checklist of todos for a thread/run ID.
     */
    TodoSnapshot saveTodos(String threadId, String runId, List<TodoItem> todos);

    default TodoSnapshot saveTodos(String threadId, List<TodoItem> todos) {
        return saveTodos(threadId, null, todos);
    }

    default boolean hasIncomplete(String threadId, String runId) {
        return listTodos(threadId, runId).stream()
                .anyMatch(todo -> !"completed".equalsIgnoreCase(todo.getStatus()));
    }

    /**
     * Clears all todos associated with a thread/run ID.
     */
    TodoSnapshot clear(String threadId, String runId);

    default TodoSnapshot clear(String threadId) {
        return clear(threadId, null);
    }

    default TodoSnapshot snapshot(String threadId, String runId) {
        return TodoSnapshot.of(threadId, runId, 0, "read", listTodos(threadId, runId));
    }
}
