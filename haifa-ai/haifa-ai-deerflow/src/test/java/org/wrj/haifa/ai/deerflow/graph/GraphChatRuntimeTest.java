package org.wrj.haifa.ai.deerflow.graph;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class GraphChatRuntimeTest {

    @Autowired
    private GraphChatRuntime graphChatRuntime;

    @Autowired
    private MessageStore messageStore;

    @MockitoBean
    private AgentModelClient modelClient;

    @Test
    void executesFullChatGraphAndSavesAnswer() {
        String runId = "run-chat-graph-" + UUID.randomUUID();
        String threadId = "thread-chat-graph-" + UUID.randomUUID();

        // Seed thread with user message
        messageStore.add(threadId, runId, MessageRole.USER, "hello deerflow graph", Map.of());

        AgentRunConfig runConfig = new AgentRunConfig(
                threadId,
                runId,
                "zhipu",
                true,
                false,
                4,
                Path.of("."),
                RunMode.CHAT,
                null,
                Map.of()
        );

        AgentRequest agentRequest = new AgentRequest(
                threadId,
                "hello deerflow graph",
                "zhipu"
        );

        AgentLoop loop = new AgentLoop(
                prompt -> Mono.just(new ModelResponse("hello user from graph")),
                new ToolRegistry(List.of())
        );
        when(modelClient.generate(any()))
                .thenReturn(Mono.just(new ModelResponse("hello user from graph")));

        GraphChatRuntimeRequest request = new GraphChatRuntimeRequest(
                loop,
                new LoopConfig(3, 2, 30_000, null),
                runConfig,
                agentRequest,
                new AtomicInteger(0),
                null,
                List.of(),
                List.of(),
                List.of()
        );

        List<AgentEvent> events = graphChatRuntime.run(request)
                .collectList()
                .block();

        assertThat(events).isNotEmpty();

        // Verify events emitted by nodes
        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.MODEL_STARTED);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.MODEL_DELTA);
            assertThat(event.content()).contains("hello user from graph");
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.RUN_COMPLETED);
            assertThat(event.content()).contains("hello user from graph");
        });

        // Verify that the assistant's final response was saved to the message store
        List<MessageRecord> history = messageStore.listByThread(threadId);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(history.get(1).content()).contains("hello user from graph");
    }

    @Test
    void activeChatToolLoopHandlesNotFoundToolAndReturnsToModel() {
        String runId = "run-chat-not-found-" + UUID.randomUUID();
        String threadId = "thread-chat-not-found-" + UUID.randomUUID();
        messageStore.add(threadId, runId, MessageRole.USER, "call unknown_tool", Map.of());

        AgentRunConfig runConfig = new AgentRunConfig(
                threadId, runId, "zhipu", true, false, 4,
                Path.of("."), RunMode.CHAT, null, Map.of()
        );
        AgentRequest agentRequest = new AgentRequest(threadId, "call unknown_tool", "zhipu");

        List<ModelPrompt> observedPrompts = new CopyOnWriteArrayList<>();
        AtomicInteger modelCalls = new AtomicInteger();
        when(modelClient.generate(any())).thenAnswer(invocation -> {
            observedPrompts.add(invocation.getArgument(0));
            if (modelCalls.getAndIncrement() == 0) {
                return Mono.just(new ModelResponse("",
                        List.of(new ModelToolCall("call-unknown-tool", "unknown_tool", "{}"))));
            }
            return Mono.just(new ModelResponse("Tool unknown_tool was not found."));
        });

        AgentLoop loop = new AgentLoop(
                prompt -> Mono.just(new ModelResponse("Tool unknown_tool was not found.")),
                new ToolRegistry(List.of())
        );

        GraphChatRuntimeRequest request = new GraphChatRuntimeRequest(
                loop, new LoopConfig(3, 2, 30_000, null),
                runConfig, agentRequest,
                new AtomicInteger(0), null, List.of(), List.of(), List.of()
        );

        List<AgentEvent> events = graphChatRuntime.run(request).collectList().block();
        assertThat(events).isNotEmpty();

        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(AgentEventType.TOOL_COMPLETED);
            assertThat(e.metadata().get("status")).isEqualTo("NOT_FOUND");
        });
        assertThat(events).anySatisfy(e -> assertThat(e.type()).isEqualTo(AgentEventType.RUN_COMPLETED));

        List<ModelPrompt> graphPrompts = observedPrompts.stream()
                .filter(prompt -> prompt.systemPrompt().contains("[Dynamic context]"))
                .toList();
        assertThat(graphPrompts).hasSize(2);
        assertThat(graphPrompts)
                .allSatisfy(prompt -> assertThat(count(prompt.systemPrompt(), "[Dynamic context]")).isEqualTo(1));
        assertThat(graphPrompts.get(1).messages())
                .anySatisfy(message -> {
                    assertThat(message.role()).isEqualTo(ModelMessage.Role.TOOL);
                    assertThat(message.name()).isEqualTo("unknown_tool");
                    assertThat(message.content()).contains("not found");
                });
    }

    private static int count(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
