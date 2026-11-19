package org.wrj.haifa.ai.deerflow.todo;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.persistence.entity.TodoItemEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.TodoItemMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.TodoItemRepository;

@Component
public class JpaTodoStore implements TodoStore {

    private final TodoItemRepository todoItemRepository;
    private final TodoItemMapper todoItemMapper;

    public JpaTodoStore(TodoItemRepository todoItemRepository, TodoItemMapper todoItemMapper) {
        this.todoItemRepository = todoItemRepository;
        this.todoItemMapper = todoItemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoItem> listTodos(String threadId, String runId) {
        if (!StringUtils.hasText(threadId) || !StringUtils.hasText(runId)) {
            return List.of();
        }
        return todoItemRepository.findByThreadIdAndRunIdOrderByOrderIndexAsc(threadId, runId).stream()
                .map(todoItemMapper::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public TodoSnapshot saveTodos(String threadId, String runId, List<TodoItem> todos) {
        if (!StringUtils.hasText(threadId) || !StringUtils.hasText(runId)) {
            return TodoSnapshot.of(threadId, runId, 0, "ignored", List.of());
        }
        if (todos == null) {
            return snapshot(threadId, runId);
        }

        List<TodoItemEntity> previous = todoItemRepository.findByThreadIdAndRunIdOrderByOrderIndexAsc(threadId, runId);
        Map<String, Instant> createdAtByTodoId = new HashMap<>();
        for (TodoItemEntity entity : previous) {
            createdAtByTodoId.put(entity.getTodoId(), entity.getCreatedAt());
        }
        int revision = previous.stream()
                .map(TodoItemEntity::getRevision)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        todoItemRepository.deleteByThreadIdAndRunId(threadId, runId);
        todoItemRepository.flush();
        Instant now = Instant.now();
        List<TodoItem> normalized = normalize(todos, now, createdAtByTodoId);
        if (!normalized.isEmpty()) {
            List<TodoItemEntity> entities = normalized.stream()
                    .map(item -> todoItemMapper.toEntity(threadId, runId, item, item.getOrderIndex(), revision,
                            item.getCreatedAt(), item.getUpdatedAt()))
                    .toList();
            todoItemRepository.saveAll(entities);
        }
        return new TodoSnapshot(threadId, runId, revision, normalized.isEmpty() ? "cleared" : "updated",
                normalized, TodoSummary.from(normalized), now);
    }

    @Override
    @Transactional
    public TodoSnapshot clear(String threadId, String runId) {
        if (!StringUtils.hasText(threadId) || !StringUtils.hasText(runId)) {
            return TodoSnapshot.of(threadId, runId, 0, "ignored", List.of());
        }
        int revision = todoItemRepository.maxRevision(threadId, runId) + 1;
        todoItemRepository.deleteByThreadIdAndRunId(threadId, runId);
        todoItemRepository.flush();
        return TodoSnapshot.of(threadId, runId, revision, "cleared", List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public TodoSnapshot snapshot(String threadId, String runId) {
        if (!StringUtils.hasText(threadId) || !StringUtils.hasText(runId)) {
            return TodoSnapshot.of(threadId, runId, 0, "read", List.of());
        }
        List<TodoItemEntity> entities = todoItemRepository.findByThreadIdAndRunIdOrderByOrderIndexAsc(threadId, runId);
        List<TodoItem> todos = entities.stream().map(todoItemMapper::toRecord).toList();
        int revision = entities.stream()
                .map(TodoItemEntity::getRevision)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0);
        Instant updatedAt = entities.stream()
                .map(TodoItemEntity::getUpdatedAt)
                .filter(value -> value != null)
                .max(Instant::compareTo)
                .orElseGet(Instant::now);
        return new TodoSnapshot(threadId, runId, revision, "read", todos, TodoSummary.from(todos), updatedAt);
    }

    private static List<TodoItem> normalize(List<TodoItem> todos, Instant now, Map<String, Instant> createdAtByTodoId) {
        return java.util.stream.IntStream.range(0, todos.size())
                .mapToObj(index -> copy(todos.get(index), index, now, createdAtByTodoId))
                .toList();
    }

    private static TodoItem copy(TodoItem source, int orderIndex, Instant now, Map<String, Instant> createdAtByTodoId) {
        TodoItem item = new TodoItem(source.getId(), source.getContent(), source.getStatus());
        item.setPriority(source.getPriority());
        item.setEvidence(source.getEvidence());
        item.setOrderIndex(orderIndex);
        item.setCreatedAt(source.getCreatedAt() != null ? source.getCreatedAt()
                : createdAtByTodoId.getOrDefault(source.getId(), now));
        item.setUpdatedAt(source.getUpdatedAt() != null ? source.getUpdatedAt() : now);
        return item;
    }
}
