package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool allowing the model to write and maintain a todo checklist.
 */
@Component
public class WriteTodosTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
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
            return ToolResult.of(name(), "Error: arguments JSON required");
        }

        try {
            JsonNode rootNode = MAPPER.readTree(jsonInput);
            JsonNode todosNode = rootNode.has("todos") ? rootNode.get("todos") : null;
            if (todosNode == null || !todosNode.isArray()) {
                return ToolResult.of(name(), "Error: 'todos' array is required");
            }

            List<TodoItem> items = new ArrayList<>();
            for (JsonNode node : todosNode) {
                String id = node.has("id") ? node.get("id").asText() : "";
                String content = node.has("content") ? node.get("content").asText() : "";
                String status = node.has("status") ? node.get("status").asText() : "pending";

                if (id.isBlank() || content.isBlank()) {
                    return ToolResult.of(name(), "Error: 'id' and 'content' are required for all todos");
                }
                items.add(new TodoItem(id, content, status));
            }

            todoStore.saveTodos(request.threadId(), request.runId(), items);

            StringBuilder sb = new StringBuilder("Current todo list updated:\n");
            for (TodoItem item : items) {
                sb.append("- [").append(item.getStatus()).append("] ").append(item.getContent()).append(" (id: ").append(item.getId()).append(")\n");
            }
            return ToolResult.of(name(), sb.toString().trim(), java.util.Map.of("todosCount", items.size()));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error updating todos: " + e.getMessage());
        }
    }
}
