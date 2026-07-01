package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.todo.InMemoryTodoStore;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;

import static org.assertj.core.api.Assertions.assertThat;

class WriteTodosToolTest {

    @Test
    void writeTodosToolCanSaveAndIsolateByThreadAndRun() {
        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool tool = new WriteTodosTool(todoStore);

        String argumentsJson1 = """
                {
                  "todos": [
                    { "id": "task-1", "content": "Gather requirements", "status": "completed" },
                    { "id": "task-2", "content": "Write code", "status": "in_progress" }
                  ]
                }
                """;

        ToolRequest request1 = new ToolRequest(argumentsJson1, Path.of("."), List.of(), "thread-1", "run-1");
        ToolResult result1 = tool.execute(request1);

        assertThat(result1.content()).contains("Gather requirements").contains("Write code");
        
        List<TodoItem> todos1 = todoStore.listTodos("thread-1", "run-1");
        assertThat(todos1).hasSize(2);
        assertThat(todos1.get(0).getId()).isEqualTo("task-1");
        assertThat(todos1.get(0).getStatus()).isEqualTo("completed");

        // Verify isolation by thread.
        List<TodoItem> todos2 = todoStore.listTodos("thread-2", "run-1");
        assertThat(todos2).isEmpty();

        // Verify isolation by run within the same thread.
        assertThat(todoStore.listTodos("thread-1", "run-2")).isEmpty();

        // Write to thread 2
        String argumentsJson2 = """
                {
                  "todos": [
                    { "id": "task-a", "content": "Write tests", "status": "pending" }
                  ]
                }
                """;
        ToolRequest request2 = new ToolRequest(argumentsJson2, Path.of("."), List.of(), "thread-2", "run-1");
        tool.execute(request2);

        assertThat(todoStore.listTodos("thread-1", "run-1")).hasSize(2);
        assertThat(todoStore.listTodos("thread-2", "run-1")).hasSize(1);
    }
}
