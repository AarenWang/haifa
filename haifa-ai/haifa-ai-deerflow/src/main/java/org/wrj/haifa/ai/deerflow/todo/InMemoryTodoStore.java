package org.wrj.haifa.ai.deerflow.todo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of TodoStore for tests and lightweight local use.
 */
public class InMemoryTodoStore implements TodoStore {

    private final Map<TodoScope, List<TodoItem>> store = new ConcurrentHashMap<>();
    private final Map<TodoScope, Integer> revisions = new ConcurrentHashMap<>();

    @Override
    public List<TodoItem> listTodos(String threadId, String runId) {
        if (threadId == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(store.getOrDefault(new TodoScope(threadId, runId), List.of()));
    }

    @Override
    public TodoSnapshot saveTodos(String threadId, String runId, List<TodoItem> todos) {
        if (threadId == null) {
            return TodoSnapshot.of(null, runId, 0, "ignored", List.of());
        }
        TodoScope scope = new TodoScope(threadId, runId);
        if (todos == null) {
            return snapshot(threadId, runId);
        }
        int revision = revisions.merge(scope, 1, Integer::sum);
        List<TodoItem> copy = copyTodos(todos, Instant.now());
        if (copy.isEmpty()) {
            store.remove(scope);
        } else {
            store.put(scope, copy);
        }
        return new TodoSnapshot(threadId, runId, revision, copy.isEmpty() ? "cleared" : "updated", copy, TodoSummary.from(copy), Instant.now());
    }

    @Override
    public TodoSnapshot clear(String threadId, String runId) {
        if (threadId == null) {
            return TodoSnapshot.of(null, runId, 0, "ignored", List.of());
        }
        TodoScope scope = new TodoScope(threadId, runId);
        int revision = revisions.merge(scope, 1, Integer::sum);
        store.remove(scope);
        return TodoSnapshot.of(threadId, runId, revision, "cleared", List.of());
    }

    @Override
    public TodoSnapshot snapshot(String threadId, String runId) {
        TodoScope scope = new TodoScope(threadId, runId);
        return TodoSnapshot.of(threadId, runId, revisions.getOrDefault(scope, 0), "read", listTodos(threadId, runId));
    }

    private static List<TodoItem> copyTodos(List<TodoItem> todos, Instant now) {
        List<TodoItem> copy = new ArrayList<>();
        for (int i = 0; i < todos.size(); i++) {
            TodoItem source = todos.get(i);
            TodoItem item = new TodoItem(source.getId(), source.getContent(), source.getStatus());
            item.setPriority(source.getPriority());
            item.setEvidence(source.getEvidence());
            item.setOrderIndex(i);
            item.setCreatedAt(source.getCreatedAt() == null ? now : source.getCreatedAt());
            item.setUpdatedAt(source.getUpdatedAt() == null ? now : source.getUpdatedAt());
            copy.add(item);
        }
        return copy;
    }

    private record TodoScope(String threadId, String runId) {
    }
}
