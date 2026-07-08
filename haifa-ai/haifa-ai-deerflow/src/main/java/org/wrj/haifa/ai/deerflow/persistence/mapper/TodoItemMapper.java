package org.wrj.haifa.ai.deerflow.persistence.mapper;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.TodoItemEntity;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;

@Component
public class TodoItemMapper {

    public TodoItem toRecord(TodoItemEntity entity) {
        TodoItem item = new TodoItem(entity.getTodoId(), entity.getContent(), entity.getStatus());
        item.setPriority(entity.getPriority());
        item.setEvidence(entity.getEvidence());
        item.setOrderIndex(entity.getOrderIndex());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    public TodoItemEntity toEntity(String threadId, String runId, TodoItem item, int orderIndex, int revision,
            Instant createdAt, Instant updatedAt) {
        TodoItemEntity entity = new TodoItemEntity();
        entity.setThreadId(threadId);
        entity.setRunId(runId);
        entity.setTodoId(item.getId());
        entity.setContent(item.getContent());
        entity.setStatus(item.getStatus());
        entity.setPriority(item.getPriority());
        entity.setEvidence(item.getEvidence());
        entity.setOrderIndex(orderIndex);
        entity.setRevision(revision);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}
