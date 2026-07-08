package org.wrj.haifa.ai.deerflow.agent.loop;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.todo.InMemoryTodoStore;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.WriteTodosTool;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class AgentLoopTodoGateIntegrationTest {

    @Test
    void rejectsFinalAnswersUntilTodosExistAndAreCompleted() {
        InMemoryTodoStore todoStore = new InMemoryTodoStore();
        AtomicInteger modelCalls = new AtomicInteger();
        AgentModelClient model = new AgentModelClient() {
            @Override
            public Mono<ModelResponse> generate(ModelPrompt prompt) {
                return switch (modelCalls.incrementAndGet()) {
                    case 1 -> Mono.just(new ModelResponse("Too early"));
                    case 2 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("todo-1", "write_todos", """
                            {"todos":[
                              {"id":"t1","content":"Gather sources","status":"in_progress"},
                              {"id":"t2","content":"Write synthesis","status":"pending"}
                            ]}
                            """))));
                    case 3 -> Mono.just(new ModelResponse("Still too early"));
                    case 4 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("todo-2", "write_todos", """
                            {"todos":[
                              {"id":"t1","content":"Gather sources","status":"completed"},
                              {"id":"t2","content":"Write synthesis","status":"completed"}
                            ]}
                            """))));
                    default -> Mono.just(new ModelResponse("Finished with completed todos"));
                };
            }
        };

        AgentLoop loop = new AgentLoop(model,
                new ToolRegistry(List.of(new WriteTodosTool(todoStore))),
                null,
                null,
                null,
                new DefaultAgentLoopObserver(todoStore));
        AgentRunConfig runConfig = new AgentRunConfig(
                "thread-todo-loop",
                "run-todo-loop",
                "test-model",
                false,
                false,
                6,
                Path.of("."),
                RunMode.RESEARCH,
                ResearchOptions.defaults(),
                Map.of());

        List<AgentEvent> events = loop.run(
                new LoopConfig(6, 8, 60_000, ResearchOptions.defaults()),
                runConfig,
                "Research system prompt",
                "Build a researched answer",
                new AtomicInteger(),
                null,
                List.of(),
                List.of()).collectList().block();

        assertThat(modelCalls.get()).isEqualTo(5);
        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.TODO_GATE_BLOCKED,
                        AgentEventType.TODO_CREATED,
                        AgentEventType.TODO_UPDATED,
                        AgentEventType.MODEL_COMPLETED,
                        AgentEventType.RUN_COMPLETED);
        assertThat(events.stream().filter(event -> event.type() == AgentEventType.TODO_GATE_BLOCKED).count())
                .isEqualTo(2);
        assertThat(events).anySatisfy(event -> {
            if (event.type() == AgentEventType.MODEL_COMPLETED) {
                assertThat(event.content()).contains("Finished with completed todos");
            }
        });
    }
}

