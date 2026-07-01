package org.wrj.haifa.ai.deerflow.agent.loop;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.MockFetchTool;
import org.wrj.haifa.ai.deerflow.tool.MockSearchTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class AgentLoopTest {

    @Test
    void mockModelRequestsSearchThenFetch() {
        // Mock model that first requests search, then fetch, then final answer
        AgentModelClient mockModel = new MockMultiTurnModelClient();

        List<AgentTool> tools = List.of(new MockSearchTool(), new MockFetchTool());
        ToolRegistry registry = new ToolRegistry(tools);
        AgentLoop loop = new AgentLoop(mockModel, registry);

        AgentRunConfig config = new AgentRunConfig("thread-1", "run-1", "test-model", false, false,
                4, Path.of("."), org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH, null, Map.of());

        List<AgentEvent> events = loop.run(
                LoopConfig.fromDefaults(),
                config,
                "You are a research assistant.",
                "Search for info then fetch article-1",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, null, null
        ).collectList().block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.MODEL_STARTED,
                        AgentEventType.MODEL_DELTA,
                        AgentEventType.TOOL_CALL_REQUESTED,
                        AgentEventType.TOOL_STARTED,
                        AgentEventType.TOOL_COMPLETED,
                        AgentEventType.RESEARCH_STEP_COMPLETED,
                        AgentEventType.MODEL_COMPLETED,
                        AgentEventType.RUN_COMPLETED);

        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.TOOL_COMPLETED) {
                assertThat(e.content()).contains("https://example.com/article-1");
            }
        });
    }

    @Test
    void stepLimitPreventsInfiniteLoop() {
        // Mock model that always requests a tool call (never gives final answer)
        AgentModelClient infiniteModel = prompt -> Mono.just(
                new ModelResponse("<tool_call name=\"mock_search\">{\"query\":\"test\"}</tool_call>"));

        List<AgentTool> tools = List.of(new MockSearchTool());
        ToolRegistry registry = new ToolRegistry(tools);
        AgentLoop loop = new AgentLoop(infiniteModel, registry);

        AgentRunConfig config = new AgentRunConfig("thread-2", "run-2", "test-model", false, false,
                4, Path.of("."), null, null, Map.of());

        LoopConfig loopConfig = new LoopConfig(3, 5, 300_000, ResearchOptions.defaults());

        List<AgentEvent> events = loop.run(
                loopConfig,
                config,
                "You are a research assistant.",
                "Infinite loop test",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, null, null
        ).collectList().block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.RUN_COMPLETED);

        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.RUN_COMPLETED) {
                assertThat(e.metadata().get("stopReason")).isEqualTo("MAX_STEPS_REACHED");
            }
        });
    }

    @Test
    void toolFailureIsRecordedAndContinues() {
        AgentTool explodingTool = new AgentTool() {
            @Override
            public String name() { return "exploding_tool"; }
            @Override
            public String description() { return "Always throws"; }
            @Override
            public boolean supports(String userMessage) { return true; }
            @Override
            public org.wrj.haifa.ai.deerflow.tool.ToolResult execute(org.wrj.haifa.ai.deerflow.tool.ToolRequest request) {
                throw new IllegalStateException("boom");
            }
        };

        AgentModelClient model = prompt -> {
            if (prompt.userPrompt().contains("exploding_tool")) {
                return Mono.just(new ModelResponse("<final_answer>Recovered from tool failure</final_answer>"));
            }
            return Mono.just(new ModelResponse("<tool_call name=\"exploding_tool\">{}</tool_call>"));
        };

        ToolRegistry registry = new ToolRegistry(List.of(explodingTool));
        AgentLoop loop = new AgentLoop(model, registry);

        AgentRunConfig config = new AgentRunConfig("thread-3", "run-3", "test-model", false, false,
                4, Path.of("."), null, null, Map.of());

        List<AgentEvent> events = loop.run(
                LoopConfig.fromDefaults(),
                config,
                "You are a research assistant.",
                "Test tool failure",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, null, null
        ).collectList().block();

        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.TOOL_COMPLETED) {
                assertThat(e.metadata().get("error")).isEqualTo("Tool failed: boom");
            }
        });
        assertThat(events).extracting(AgentEvent::type).contains(AgentEventType.MODEL_COMPLETED);
    }

    @Test
    void maxToolCallsLimitIsEnforced() {
        AgentModelClient model = prompt -> Mono.just(
                new ModelResponse("<tool_call name=\"mock_search\">{\"query\":\"test\"}</tool_call>"));

        List<AgentTool> tools = List.of(new MockSearchTool());
        ToolRegistry registry = new ToolRegistry(tools);
        AgentLoop loop = new AgentLoop(model, registry);

        AgentRunConfig config = new AgentRunConfig("thread-4", "run-4", "test-model", false, false,
                4, Path.of("."), null, null, Map.of());

        LoopConfig loopConfig = new LoopConfig(20, 2, 300_000, ResearchOptions.defaults());

        List<AgentEvent> events = loop.run(
                loopConfig,
                config,
                "You are a research assistant.",
                "Max tool calls test",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, null, null
        ).collectList().block();

        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.RUN_COMPLETED) {
                assertThat(e.metadata().get("stopReason")).isEqualTo("MAX_TOOL_CALLS_REACHED");
            }
        });
    }

    @Test
    void executesToolCallsFromSameModelResponseConcurrently() {
        SlowTool slowTool = new SlowTool();
        AtomicInteger modelCalls = new AtomicInteger();
        AgentModelClient model = prompt -> {
            if (modelCalls.incrementAndGet() == 1) {
                return Mono.just(new ModelResponse("", List.of(
                        new ModelToolCall("call-1", "slow_tool", "{\"value\":\"a\"}"),
                        new ModelToolCall("call-2", "slow_tool", "{\"value\":\"b\"}")
                )));
            }
            return Mono.just(new ModelResponse("<final_answer>Both slow tools finished</final_answer>"));
        };

        AgentLoop loop = new AgentLoop(model, new ToolRegistry(List.of(slowTool)));
        AgentRunConfig config = new AgentRunConfig("thread-parallel", "run-parallel", "test-model",
                false, false, 4, Path.of("."), org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH,
                null, Map.of());

        List<AgentEvent> events = loop.run(
                new LoopConfig(4, 4, 300_000, ResearchOptions.defaults()),
                config,
                "You are a research assistant.",
                "Run both tools",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, List.of(), List.of()
        ).collectList().block();

        assertThat(slowTool.maxActive.get()).isGreaterThanOrEqualTo(2);
        assertThat(events.stream().filter(e -> e.type() == AgentEventType.TOOL_COMPLETED).count()).isEqualTo(2);
    }

    @Test
    void retriesWhenModelEmitsMalformedToolCallIntent() {
        AtomicInteger modelCalls = new AtomicInteger();
        AgentModelClient model = prompt -> switch (modelCalls.incrementAndGet()) {
            case 1 -> Mono.just(new ModelResponse("Tool call - mock_search({\"query\":\"lishui\"})"));
            case 2 -> Mono.just(new ModelResponse("<tool_call name=\"mock_search\">{\"query\":\"lishui\"}</tool_call>"));
            default -> Mono.just(new ModelResponse("<final_answer>Search completed after format correction.</final_answer>"));
        };

        AgentLoop loop = new AgentLoop(model, new ToolRegistry(List.of(new MockSearchTool())));
        AgentRunConfig config = new AgentRunConfig("thread-correct", "run-correct", "test-model",
                false, false, 5, Path.of("."), org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT,
                null, Map.of());

        List<AgentEvent> events = loop.run(
                new LoopConfig(5, 5, 300_000, ResearchOptions.defaults()),
                config,
                "You are a helpful assistant.",
                "Search for Lishui",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, List.of(), List.of()
        ).collectList().block();

        assertThat(modelCalls.get()).isEqualTo(3);
        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.TOOL_CALL_REQUESTED
                    && Boolean.TRUE.equals(e.metadata().get("unparsedToolCall"))) {
                assertThat(e.content()).contains("Unparsed tool call format detected");
            }
        });
        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.TOOL_COMPLETED) {
                assertThat(e.metadata().get("toolName")).isEqualTo("mock_search");
            }
        });
        assertThat(events).anySatisfy(e -> {
            if (e.type() == AgentEventType.MODEL_COMPLETED) {
                assertThat(e.content()).contains("Search completed after format correction");
            }
        });
    }

    @Test
    void emitsEventsBeforeModelCallCompletes() throws Exception {
        CountDownLatch modelStarted = new CountDownLatch(1);
        CountDownLatch releaseModel = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AgentModelClient slowModel = prompt -> Mono.fromCallable(() -> {
            releaseModel.await(3, TimeUnit.SECONDS);
            return new ModelResponse("<final_answer>Delayed answer</final_answer>");
        });

        AgentLoop loop = new AgentLoop(slowModel, new ToolRegistry(List.of()));
        AgentRunConfig config = new AgentRunConfig("thread-stream", "run-stream", "test-model",
                false, false, 2, Path.of("."), org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT,
                null, Map.of());

        loop.run(
                new LoopConfig(2, 2, 300_000, ResearchOptions.defaults()),
                config,
                "You are a helpful assistant.",
                "Return later",
                new java.util.concurrent.atomic.AtomicInteger(),
                null, List.of(), List.of()
        ).subscribe(
                event -> {
                    if (event.type() == AgentEventType.MODEL_STARTED) {
                        modelStarted.countDown();
                    }
                },
                ex -> {
                    error.set(ex);
                    done.countDown();
                },
                done::countDown
        );

        assertThat(modelStarted.await(500, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(releaseModel.getCount()).isEqualTo(1);

        releaseModel.countDown();
        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();
    }

    /**
     * Mock model that simulates a multi-turn conversation:
     * Turn 1: requests search
     * Turn 2: requests fetch
     * Turn 3: provides final answer
     */
    private static class MockMultiTurnModelClient implements AgentModelClient {
        private int callCount = 0;

        @Override
        public Mono<ModelResponse> generate(ModelPrompt prompt) {
            callCount++;
            return switch (callCount) {
                case 1 -> Mono.just(new ModelResponse("<tool_call name=\"mock_search\">{\"query\":\"test\"}</tool_call>"));
                case 2 -> Mono.just(new ModelResponse("<tool_call name=\"mock_fetch\">{\"url\":\"https://example.com/article-1\"}</tool_call>"));
                case 3 -> Mono.just(new ModelResponse("<final_answer>Based on the search and fetch results, here is the answer.</final_answer>"));
                default -> Mono.just(new ModelResponse("<final_answer>Default answer.</final_answer>"));
            };
        }
    }

    private static class SlowTool implements AgentTool {
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();

        @Override
        public String name() {
            return "slow_tool";
        }

        @Override
        public String description() {
            return "Sleeps briefly so tests can observe concurrent execution";
        }

        @Override
        public boolean supports(String userMessage) {
            return true;
        }

        @Override
        public org.wrj.haifa.ai.deerflow.tool.ToolResult execute(org.wrj.haifa.ai.deerflow.tool.ToolRequest request) {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            try {
                Thread.sleep(150);
                return org.wrj.haifa.ai.deerflow.tool.ToolResult.of(name(), "done " + request.userMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return org.wrj.haifa.ai.deerflow.tool.ToolResult.of(name(), "interrupted");
            } finally {
                active.decrementAndGet();
            }
        }
    }
}
