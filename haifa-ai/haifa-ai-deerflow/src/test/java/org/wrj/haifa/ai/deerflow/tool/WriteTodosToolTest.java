package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.todo.InMemoryTodoStore;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoSnapshot;
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

        assertThat(result1.content()).contains("Todo list created");
        assertThat(result1.metadata()).containsEntry("todoOperation", "created");
        assertThat(result1.metadata()).containsEntry("todosCount", 2);
        assertThat(result1.metadata()).containsEntry("completedCount", 1L);
        assertThat(result1.metadata()).containsEntry("inProgressCount", 1L);
        assertThat(result1.metadata()).containsKey("snapshot");
        TodoSnapshot snapshot = (TodoSnapshot) result1.metadata().get("snapshot");
        assertThat(snapshot.todos()).extracting(TodoItem::getContent)
                .containsExactly("Gather requirements", "Write code");
        assertThat(snapshot.summary().completed()).isEqualTo(1);
        assertThat(snapshot.summary().inProgress()).isEqualTo(1);

        List<TodoItem> todos1 = todoStore.listTodos("thread-1", "run-1");
        assertThat(todos1).hasSize(2);
        assertThat(todos1.get(0).getId()).isEqualTo("task-1");
        assertThat(todos1.get(0).getStatus()).isEqualTo("completed");

        assertThat(todoStore.listTodos("thread-2", "run-1")).isEmpty();
        assertThat(todoStore.listTodos("thread-1", "run-2")).isEmpty();

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

    @Test
    void writeTodosToolCanReadCurrentSnapshotWithoutMutation() {
        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool tool = new WriteTodosTool(todoStore);
        todoStore.saveTodos("thread-1", "run-1", List.of(
                new TodoItem("task-1", "Already planned", "pending")
        ));

        ToolResult result = tool.execute(new ToolRequest("{}", Path.of("."), List.of(), "thread-1", "run-1"));

        assertThat(result.content()).contains("Todo list read");
        assertThat(result.metadata()).containsEntry("todoOperation", "read");
        TodoSnapshot snapshot = (TodoSnapshot) result.metadata().get("snapshot");
        assertThat(snapshot.todos()).hasSize(1);
        assertThat(snapshot.todos().get(0).getContent()).isEqualTo("Already planned");
    }

    @Test
    void writeTodosToolCanClearTodosWithEmptyList() {
        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool tool = new WriteTodosTool(todoStore);
        todoStore.saveTodos("thread-1", "run-1", List.of(
                new TodoItem("task-1", "Already planned", "pending")
        ));

        ToolResult result = tool.execute(new ToolRequest("{\"todos\":[]}", Path.of("."), List.of(), "thread-1", "run-1"));

        assertThat(result.content()).contains("Todo list cleared");
        assertThat(result.metadata()).containsEntry("todoOperation", "cleared");
        assertThat(todoStore.listTodos("thread-1", "run-1")).isEmpty();
    }

    @Test
    void writeTodosToolRejectsMultipleInProgressItems() {
        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool tool = new WriteTodosTool(todoStore);

        ToolResult result = tool.execute(new ToolRequest("""
                {
                  "todos": [
                    { "id": "task-1", "content": "One", "status": "in_progress" },
                    { "id": "task-2", "content": "Two", "status": "in_progress" }
                  ]
                }
                """, Path.of("."), List.of(), "thread-1", "run-1"));

        assertThat(result.content()).contains("Error").contains("only one todo");
        assertThat(result.metadata()).containsEntry("error", true);
        assertThat(todoStore.listTodos("thread-1", "run-1")).isEmpty();
    }

    @Test
    void writeTodosToolRejectsInvalidStatus() {
        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool tool = new WriteTodosTool(todoStore);

        ToolResult result = tool.execute(new ToolRequest("""
                {
                  "todos": [
                    { "id": "task-1", "content": "One", "status": "blocked" }
                  ]
                }
                """, Path.of("."), List.of(), "thread-1", "run-1"));

        assertThat(result.content()).contains("Error").contains("invalid status");
        assertThat(result.metadata()).containsEntry("error", true);
        assertThat(todoStore.listTodos("thread-1", "run-1")).isEmpty();
    }

    @Test
    void writeTodosToolRequiresReasonBeforeDowngradingCompletedItem() {
        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool tool = new WriteTodosTool(todoStore);
        todoStore.saveTodos("thread-1", "run-1", List.of(
                new TodoItem("task-1", "Already done", "completed")
        ));

        ToolResult rejected = tool.execute(new ToolRequest("""
                {
                  "todos": [
                    { "id": "task-1", "content": "Already done", "status": "pending" }
                  ]
                }
                """, Path.of("."), List.of(), "thread-1", "run-1"));

        assertThat(rejected.content()).contains("cannot be downgraded");
        assertThat(todoStore.listTodos("thread-1", "run-1").get(0).getStatus()).isEqualTo("completed");

        ToolResult accepted = tool.execute(new ToolRequest("""
                {
                  "reason": "New evidence requires rework",
                  "todos": [
                    { "id": "task-1", "content": "Already done", "status": "pending" }
                  ]
                }
                """, Path.of("."), List.of(), "thread-1", "run-1"));

        assertThat(accepted.content()).contains("Todo list updated");
        assertThat(todoStore.listTodos("thread-1", "run-1").get(0).getStatus()).isEqualTo("pending");
    }
}
