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
    void saveTodos(String threadId, String runId, List<TodoItem> todos);

    default void saveTodos(String threadId, List<TodoItem> todos) {
        saveTodos(threadId, null, todos);
    }

    /**
     * Clears all todos associated with a thread/run ID.
     */
    void clear(String threadId, String runId);

    default void clear(String threadId) {
        clear(threadId, null);
    }
}
