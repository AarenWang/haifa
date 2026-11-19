package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoSnapshot;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import org.wrj.haifa.ai.deerflow.todo.TodoSummary;

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
        return "Create, read, or update a structured todo checklist for complex multi-step objectives. "
                + "Arguments: {\"todos\": [{\"id\": \"task-id\", \"content\": \"task description\", \"status\": \"pending|in_progress|completed|cancelled\"}]}. "
                + "Omit the todos array to read the current checklist.";
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
            if (todosNode == null || todosNode.isNull()) {
                TodoSnapshot snapshot = withOperation(todoStore.snapshot(request.threadId(), request.runId()), "read");
                return success(snapshot, List.of(), "Todo list read: " + snapshot.summary().total() + " item(s)");
            }
            if (!todosNode.isArray()) {
                return error("'todos' must be an array when provided");
            }

            List<TodoItem> items = new ArrayList<>();
            Instant now = Instant.now();
            int orderIndex = 0;
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
                item.setOrderIndex(orderIndex++);
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

            String operation = items.isEmpty() ? "cleared" : (previous.isEmpty() ? "created" : "updated");
            List<Map<String, Object>> actions = buildActions(previous, items);
            TodoSnapshot savedSnapshot = todoStore.saveTodos(request.threadId(), request.runId(), items);
            TodoSnapshot snapshot = withOperation(savedSnapshot, operation);

            String content = switch (operation) {
                case "created" -> "Todo list created: " + items.size() + " item(s)";
                case "cleared" -> "Todo list cleared";
                default -> "Todo list updated: " + snapshot.summary().completed() + " completed, "
                        + snapshot.summary().inProgress() + " in progress, " + snapshot.summary().pending() + " pending";
            };
            return success(snapshot, actions, content);
        } catch (Exception e) {
            return error("invalid JSON: " + e.getMessage());
        }
    }

    private ToolResult success(TodoSnapshot snapshot, List<Map<String, Object>> actions, String content) {
        TodoSummary summary = snapshot.summary();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("todoOperation", snapshot.operation());
        metadata.put("revision", snapshot.revision());
        metadata.put("snapshot", snapshot);
        metadata.put("todos", snapshot.todos());
        metadata.put("summary", summary);
        metadata.put("actions", actions == null ? List.of() : actions);
        metadata.put("todosCount", summary.total());
        metadata.put("pendingCount", (long) summary.pending());
        metadata.put("inProgressCount", (long) summary.inProgress());
        metadata.put("completedCount", (long) summary.completed());
        metadata.put("cancelledCount", (long) summary.cancelled());
        return ToolResult.of(name(), content, metadata);
    }

    private static ToolResult error(String message) {
        return ToolResult.of("write_todos", "Error: " + message, Map.of("error", true, "reason", message));
    }

    private static TodoSnapshot withOperation(TodoSnapshot snapshot, String operation) {
        return new TodoSnapshot(snapshot.threadId(), snapshot.runId(), snapshot.revision(), operation,
                snapshot.todos(), snapshot.summary(), snapshot.updatedAt());
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

    private static List<Map<String, Object>> buildActions(List<TodoItem> previous, List<TodoItem> next) {
        Map<String, TodoItem> previousById = previous == null ? Map.of() : previous.stream()
                .collect(Collectors.toMap(TodoItem::getId, Function.identity(), (left, right) -> right));
        Map<String, TodoItem> nextById = next == null ? Map.of() : next.stream()
                .collect(Collectors.toMap(TodoItem::getId, Function.identity(), (left, right) -> right));
        List<Map<String, Object>> actions = new ArrayList<>();
        for (TodoItem item : next == null ? List.<TodoItem>of() : next) {
            TodoItem old = previousById.get(item.getId());
            if (old == null) {
                actions.add(action("created", item.getId(), null, item.getStatus(), item.getContent()));
            } else if (!normalizeStatus(old.getStatus()).equals(normalizeStatus(item.getStatus()))) {
                actions.add(action("status_changed", item.getId(), old.getStatus(), item.getStatus(), item.getContent()));
            } else if (!String.valueOf(old.getContent()).equals(String.valueOf(item.getContent()))) {
                actions.add(action("content_changed", item.getId(), old.getStatus(), item.getStatus(), item.getContent()));
            }
        }
        for (TodoItem item : previous == null ? List.<TodoItem>of() : previous) {
            if (!nextById.containsKey(item.getId())) {
                actions.add(action("removed", item.getId(), item.getStatus(), null, item.getContent()));
            }
        }
        return actions;
    }

    private static Map<String, Object> action(String kind, String todoId, String fromStatus, String toStatus, String content) {
        Map<String, Object> action = new HashMap<>();
        action.put("kind", kind);
        action.put("todoId", todoId);
        action.put("fromStatus", fromStatus);
        action.put("toStatus", toStatus);
        action.put("content", content);
        return action;
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
