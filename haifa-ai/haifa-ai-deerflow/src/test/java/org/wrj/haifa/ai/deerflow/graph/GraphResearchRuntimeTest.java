package org.wrj.haifa.ai.deerflow.graph;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderRegistry;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderRegistry;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class GraphResearchRuntimeTest {

    @Autowired
    private GraphResearchRuntime graphResearchRuntime;

    @Autowired
    private ResearchPlanStore planStore;

    @Autowired
    private ResearchRuntimeSupport researchRuntimeSupport;

    @MockitoBean
    private AgentModelClient modelClient;

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
        when(modelClient.generate(any()))
                .thenReturn(
                        Mono.just(new ModelResponse("not-json-plan")),
                        Mono.just(new ModelResponse("deepseek analysis synthesis")));

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
        assertThat(researchRuntimeSupport.listSourcesByRun(runId)).isNotEmpty();
        assertThat(researchRuntimeSupport.listEvidenceByRun(runId)).isNotEmpty();
    }

    private static WebSearchProvider searchProvider() {
        return new WebSearchProvider() {
            @Override
            public WebSearchProviderType type() {
                return WebSearchProviderType.ALIYUN;
            }

            @Override
            public String search(String query, int maxResults) {
                return """
                        1. [DeepSeek architecture overview] https://example.com/deepseek-architecture
                        Summary: DeepSeek architecture and implementation overview for graph research.
                        2. [DeepSeek market adoption] https://example.com/deepseek-market
                        Summary: DeepSeek market adoption and software ecosystem trends.
                        """;
            }
        };
    }

    private static WebFetchProvider fetchProvider() {
        return new WebFetchProvider() {
            @Override
            public WebFetchProviderType type() {
                return WebFetchProviderType.ALIYUN;
            }

            @Override
            public String fetch(String url) {
                return """
                        DeepSeek technology software combines model serving, developer tooling, and application integration patterns.
                        According to the source, adoption depends on reliability, cost controls, and practical implementation support.
                        Market analysis suggests teams evaluate DeepSeek alongside other AI model providers before production rollout.
                        """;
            }
        };
    }

    @TestConfiguration
    static class OfflineProviderConfig {

        @Bean
        @Primary
        WebSearchProviderRegistry testSearchRegistry() {
            return new WebSearchProviderRegistry(List.of(searchProvider()));
        }

        @Bean
        @Primary
        WebFetchProviderRegistry testFetchRegistry() {
            return new WebFetchProviderRegistry(List.of(fetchProvider()));
        }
    }
}
