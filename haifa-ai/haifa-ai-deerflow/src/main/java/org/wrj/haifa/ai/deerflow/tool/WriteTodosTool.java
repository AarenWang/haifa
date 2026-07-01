package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;

/**
 * Tool allowing the model to write and maintain a todo checklist.
 */
@Component
public class WriteTodosTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> VALID_STATUSES = Set.of("pending", "in_progress", "completed", "cancelled");
    private final TodoStore todoStore;

    public WriteTodosTool(TodoStore todoStore) {
        this.todoStore = todoStore;
    }

    @Override
    public String name() {
        return "write_todos";
    }

    @Override
    public String description() {
        return "Create or update a structured todo checklist for complex multi-step objectives. "
                + "Arguments: {\"todos\": [{\"id\": \"task-id\", \"content\": \"task description\", \"status\": \"pending|in_progress|completed\"}]}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("write_todos");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String jsonInput = request.userMessage();
        if (jsonInput == null || jsonInput.isBlank()) {
            return error("arguments JSON required");
        }

        try {
            JsonNode rootNode = MAPPER.readTree(jsonInput);
            JsonNode todosNode = rootNode.has("todos") ? rootNode.get("todos") : null;
            if (todosNode == null || !todosNode.isArray()) {
                return error("'todos' array is required");
            }

            List<TodoItem> items = new ArrayList<>();
            Instant now = Instant.now();
            for (JsonNode node : todosNode) {
                String id = node.has("id") ? node.get("id").asText() : "";
                String content = node.has("content") ? node.get("content").asText() : "";
                String status = normalizeStatus(node.has("status") ? node.get("status").asText() : "pending");

                if (id.isBlank() || content.isBlank()) {
                    return error("'id' and 'content' are required for all todos");
                }
                if (!VALID_STATUSES.contains(status)) {
                    return error("invalid status '" + status + "'. Allowed values: " + String.join(", ", VALID_STATUSES));
                }

                TodoItem item = new TodoItem(id, content, status);
                item.setPriority(textOrNull(node, "priority"));
                item.setEvidence(textOrNull(node, "evidence"));
                item.setUpdatedAt(now);
                items.add(item);
            }

            long inProgressCount = countByStatus(items, "in_progress");
            if (inProgressCount > 1) {
                return error("only one todo may be in_progress at a time");
            }

            List<TodoItem> previous = todoStore.listTodos(request.threadId(), request.runId());
            String downgradeError = validateCompletedDowngrades(previous, items, rootNode);
            if (downgradeError != null) {
                return error(downgradeError);
            }

            todoStore.saveTodos(request.threadId(), request.runId(), items);

            String operation = previous.isEmpty() ? "created" : "updated";
            StringBuilder sb = new StringBuilder("Todo list ").append(operation).append(":\n");
            for (TodoItem item : items) {
                sb.append("- [").append(item.getStatus()).append("] ").append(item.getContent()).append(" (id: ").append(item.getId()).append(")\n");
            }
            return ToolResult.of(name(), sb.toString().trim(), Map.of(
                    "todosCount", items.size(),
                    "pendingCount", countByStatus(items, "pending"),
                    "inProgressCount", inProgressCount,
                    "completedCount", countByStatus(items, "completed"),
                    "cancelledCount", countByStatus(items, "cancelled"),
                    "todoOperation", operation
            ));
        } catch (Exception e) {
            return error("invalid JSON: " + e.getMessage());
        }
    }

    private static ToolResult error(String message) {
        return ToolResult.of("write_todos", "Error: " + message, Map.of("error", true, "reason", message));
    }

    private static String normalizeStatus(String status) {
        return status == null ? "pending" : status.trim().toLowerCase(Locale.ROOT);
    }

    private static String textOrNull(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value;
    }

    private static long countByStatus(List<TodoItem> todos, String status) {
        return todos.stream()
                .filter(todo -> status.equalsIgnoreCase(todo.getStatus()))
                .count();
    }

    private static String validateCompletedDowngrades(List<TodoItem> previous, List<TodoItem> next, JsonNode rootNode) {
        if (previous == null || previous.isEmpty()) {
            return null;
        }
        boolean hasGlobalReason = rootNode.has("reason") && !rootNode.get("reason").asText("").isBlank();
        for (TodoItem oldItem : previous) {
            if (!"completed".equalsIgnoreCase(oldItem.getStatus())) {
                continue;
            }
            TodoItem nextItem = next.stream()
                    .filter(candidate -> oldItem.getId().equals(candidate.getId()))
                    .findFirst()
                    .orElse(null);
            if (nextItem == null || "completed".equalsIgnoreCase(nextItem.getStatus())) {
                continue;
            }
            boolean hasItemReason = findNodeById(rootNode.get("todos"), nextItem.getId())
                    .map(node -> node.has("reason") && !node.get("reason").asText("").isBlank())
                    .orElse(false);
            if (!hasGlobalReason && !hasItemReason) {
                return "completed todo '" + nextItem.getId() + "' cannot be downgraded without a reason";
            }
        }
        return null;
    }

    private static java.util.Optional<JsonNode> findNodeById(JsonNode todosNode, String id) {
        if (todosNode == null || !todosNode.isArray()) {
            return java.util.Optional.empty();
        }
        for (JsonNode node : todosNode) {
            if (node.has("id") && id.equals(node.get("id").asText())) {
                return java.util.Optional.of(node);
            }
        }
        return java.util.Optional.empty();
    }
}
