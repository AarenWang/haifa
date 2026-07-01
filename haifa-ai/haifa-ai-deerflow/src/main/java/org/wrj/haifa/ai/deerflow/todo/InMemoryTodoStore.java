package org.wrj.haifa.ai.deerflow.todo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory thread-safe implementation of TodoStore.
 */
@Component
public class InMemoryTodoStore implements TodoStore {

    private final Map<TodoScope, List<TodoItem>> store = new ConcurrentHashMap<>();

    @Override
    public List<TodoItem> listTodos(String threadId, String runId) {
        if (threadId == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(store.getOrDefault(new TodoScope(threadId, runId), List.of()));
    }

    @Override
    public void saveTodos(String threadId, String runId, List<TodoItem> todos) {
        if (threadId == null) {
            return;
        }
        if (todos == null) {
            store.remove(new TodoScope(threadId, runId));
        } else {
            store.put(new TodoScope(threadId, runId), new ArrayList<>(todos));
        }
    }

    @Override
    public void clear(String threadId, String runId) {
        if (threadId != null) {
            store.remove(new TodoScope(threadId, runId));
        }
    }

    private record TodoScope(String threadId, String runId) {
    }
}
