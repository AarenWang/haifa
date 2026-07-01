package org.wrj.haifa.ai.deerflow.middleware;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.todo.InMemoryTodoStore;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class TodoMiddlewareTest {

    @Test
    void todoMiddlewareInjectsInstructionsWhenNoTodos() {
        TodoStore todoStore = new InMemoryTodoStore();
        TodoMiddleware middleware = new TodoMiddleware(todoStore);

        AgentRunConfig runConfig = new AgentRunConfig("thread-1", "run-1", "test-model", false, false,
                4, Path.of("."), Map.of());
        AgentRequest request = new AgentRequest("thread-1", "Hello", "test-model");
        AgentRuntimeContext context = AgentRuntimeContext.of(runConfig, request, List.of(), new DeerFlowProperties());

        ModelPrompt prompt = new ModelPrompt("Original system prompt", "User request", "test-model");
        MiddlewareChain chain = new MiddlewareChain(List.of(middleware)) {
            @Override
            public Mono<ModelPrompt> next(AgentRuntimeContext ctx) {
                return Mono.just(prompt);
            }
        };

        ModelPrompt processed = middleware.apply(context, chain).block();

        assertThat(processed.systemPrompt()).contains("write_todos").contains("Original system prompt");
        assertThat(processed.userPrompt()).isEqualTo("User request");
    }

    @Test
    void todoMiddlewareInjectsRemindersWhenTodosExist() {
        TodoStore todoStore = new InMemoryTodoStore();
        todoStore.saveTodos("thread-2", "run-2", List.of(
                new TodoItem("t1", "Do research", "pending"),
                new TodoItem("t2", "Write report", "in_progress")
        ));
        todoStore.saveTodos("thread-2", "other-run", List.of(
                new TodoItem("old", "Old run task", "pending")
        ));
        TodoMiddleware middleware = new TodoMiddleware(todoStore);

        AgentRunConfig runConfig = new AgentRunConfig("thread-2", "run-2", "test-model", false, false,
                4, Path.of("."), Map.of());
        AgentRequest request = new AgentRequest("thread-2", "Hello", "test-model");
        AgentRuntimeContext context = AgentRuntimeContext.of(runConfig, request, List.of(), new DeerFlowProperties());

        ModelPrompt prompt = new ModelPrompt("Original system prompt", "User request", "test-model");
        MiddlewareChain chain = new MiddlewareChain(List.of(middleware)) {
            @Override
            public Mono<ModelPrompt> next(AgentRuntimeContext ctx) {
                return Mono.just(prompt);
            }
        };

        ModelPrompt processed = middleware.apply(context, chain).block();

        assertThat(processed.systemPrompt()).isEqualTo("Original system prompt");
        assertThat(processed.userPrompt()).contains("todo_state").contains("Do research").contains("Write report");
        assertThat(processed.userPrompt()).doesNotContain("Old run task");
    }
}
