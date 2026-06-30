package org.wrj.haifa.ai.deerflow.agent.loop;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
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
                4, Path.of("."), null, null, Map.of());

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
}
