package org.wrj.haifa.ai.deerflow.graph;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GraphResearchRuntimeTest {

    @Autowired
    private GraphResearchRuntime graphResearchRuntime;

    @Autowired
    private ResearchPlanStore planStore;

    @Test
    void executesFullResearchGraphAndEmitsEvents() {
        String runId = "run-research-graph-" + UUID.randomUUID();
        String threadId = "thread-research-graph-" + UUID.randomUUID();

        AgentRunConfig runConfig = new AgentRunConfig(
                threadId,
                runId,
                "zhipu",
                true,
                false,
                10,
                Path.of("."),
                RunMode.RESEARCH,
                ResearchOptions.defaults(),
                Map.of()
        );

        AgentRequest agentRequest = new AgentRequest(
                threadId,
                "deepseek technology software",
                "zhipu"
        );

        AgentLoop loop = new AgentLoop(
                prompt -> Mono.just(new ModelResponse("<final_answer>deepseek analysis synthesis</final_answer>")),
                new ToolRegistry(List.of())
        );

        GraphResearchRuntimeRequest request = new GraphResearchRuntimeRequest(
                loop,
                new LoopConfig(3, 2, 30_000, ResearchOptions.defaults()),
                runConfig,
                agentRequest,
                "You are research assistant",
                "deepseek technology software",
                new AtomicInteger(0),
                null,
                List.of(),
                List.of(),
                List.of()
        );

        // Verify state machine execution via events emitted
        List<AgentEvent> events = graphResearchRuntime.run(request)
                .collectList()
                .block();

        assertThat(events).isNotEmpty();

        // Verify specific events from our nodes
        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.RESEARCH_PLAN_CREATED);
            assertThat(event.runId()).isEqualTo(runId);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.SOURCE_FOUND);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.SOURCE_FETCHED);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.QUALITY_GATE_STARTED);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.REPORT_COMPLETED);
        });

        // Verify the plan is in the database/store
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        assertThat(plan).isNotNull();
        assertThat(plan.topic()).isEqualTo("deepseek technology software");
    }
}
