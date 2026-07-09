package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
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
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.AgentGraphCheckpointRecord;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.SQLiteCheckpointSaver;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderRegistry;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderRegistry;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Instant;
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

    @Autowired
    private DeerFlowProperties properties;

    @Autowired
    private SQLiteCheckpointSaver checkpointSaver;

    @Autowired
    private AgentGraphCheckpointStore checkpointStore;

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
                prompt -> Mono.just(new ModelResponse("deepseek analysis synthesis")),
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
            assertThat(event.type()).isEqualTo(AgentEventType.TODO_CREATED);
        });

        assertThat(events).anySatisfy(event -> {
            assertThat(event.type()).isEqualTo(AgentEventType.SUBAGENT_STARTED);
            assertThat(event.metadata()).containsEntry("dispatchMode", "dimension_graph");
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
        assertThat(planStore.findTasksByRunId(runId))
                .isNotEmpty()
                .allSatisfy(task -> assertThat(task.runId()).isEqualTo(runId));
        assertThat(researchRuntimeSupport.listSourcesByRun(runId)).isNotEmpty();
        assertThat(researchRuntimeSupport.listEvidenceByRun(runId)).isNotEmpty();
    }

    @Test
    void checkpointEnabledSavesResearchCheckpointsWithDiagnosticSummary() {
        boolean originalCheckpointEnabled = properties.getGraph().getCheckpoint().isEnabled();
        properties.getGraph().getCheckpoint().setEnabled(true);

        try {
            String runId = "run-research-checkpoint-" + UUID.randomUUID();
            String threadId = "thread-research-checkpoint-" + UUID.randomUUID();
            GraphResearchRuntimeRequest request = request(runId, threadId, new LoopConfig(1, 2, 30_000, ResearchOptions.quick()));

            when(modelClient.generate(any()))
                    .thenReturn(
                            Mono.just(new ModelResponse("not-json-plan")),
                            Mono.just(new ModelResponse("checkpoint synthesis")));

            List<AgentEvent> events = graphResearchRuntime.run(request)
                    .collectList()
                    .block();

            assertThat(events).anySatisfy(event -> assertThat(event.type()).isEqualTo(AgentEventType.REPORT_COMPLETED));

            List<AgentGraphCheckpointRecord> records = checkpointStore.findByRunId(runId);
            assertThat(records).isNotEmpty();
            AgentGraphCheckpointRecord last = records.get(records.size() - 1);
            assertThat(last.nextNodeId()).isBlank();
            assertThat(last.stateSummary())
                    .containsEntry("runId", runId)
                    .containsEntry("threadId", threadId)
                    .containsEntry("graphName", "haifa-active-research")
                    .containsEntry("nodeId", "write_report")
                    .containsEntry("nextNodeId", "")
                    .containsEntry("researchPhase", "completed")
                    .containsKey("sourceCount")
                    .containsKey("evidenceCount")
                    .containsKey("qualityGatePassed")
                    .containsKey("artifactCount");
            assertThat(last.fullState())
                    .containsKeys(AgentGraphStateKeys.TODOS,
                            AgentGraphStateKeys.SUBAGENTS,
                            AgentGraphStateKeys.CITATION_VERIFICATION);
        }
        finally {
            properties.getGraph().getCheckpoint().setEnabled(originalCheckpointEnabled);
        }
    }

    @Test
    void resumesResearchGraphFromCheckpointWithoutRecreatingPlan() {
        boolean originalCheckpointEnabled = properties.getGraph().getCheckpoint().isEnabled();
        properties.getGraph().getCheckpoint().setEnabled(true);

        try {
            String runId = "run-research-resume-" + UUID.randomUUID();
            String threadId = "thread-research-resume-" + UUID.randomUUID();
            ResearchPlan existingPlan = planStore.save(plan(runId, threadId, "resume topic"));

            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
            checkpointSaver.put(runnableConfig, Checkpoint.builder()
                    .id("cp-research-resume-" + UUID.randomUUID())
                    .nodeId("create_or_load_plan")
                    .nextNodeId("search_sources")
                    .state(Map.ofEntries(
                            Map.entry(AgentGraphStateKeys.RUN_ID, runId),
                            Map.entry(AgentGraphStateKeys.THREAD_ID, threadId),
                            Map.entry(AgentGraphStateKeys.MODE, RunMode.RESEARCH.name()),
                            Map.entry(AgentGraphStateKeys.USER_MESSAGE, existingPlan.topic()),
                            Map.entry(AgentGraphStateKeys.MODEL_NAME, "zhipu"),
                            Map.entry(AgentGraphStateKeys.RESEARCH_OPTIONS, Map.of(
                                    "depth", "quick",
                                    "timeWindow", "latest",
                                    "maxSources", 5,
                                    "requireCitations", false,
                                    "outputFormat", "answer"
                            )),
                            Map.entry(AgentGraphStateKeys.RESEARCH_STEPS, 0),
                            Map.entry(AgentGraphStateKeys.QUALITY_GATE_PASSED, false),
                            Map.entry(AgentGraphStateKeys.EMITTED_EVIDENCE_IDS, List.of()),
                            Map.entry(AgentGraphStateKeys.RESEARCH_PHASE, "planning"),
                            Map.entry(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, 0),
                            Map.entry(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, 0),
                            Map.entry(AgentGraphStateKeys.MODEL_STEPS, List.of()),
                            Map.entry(AgentGraphStateKeys.MESSAGE_WINDOW, List.of()),
                            Map.entry(AgentGraphStateKeys.TOOL_CALLS, List.of()),
                            Map.entry(AgentGraphStateKeys.TOOL_RESULTS, List.of()),
                            Map.entry(AgentGraphStateKeys.ARTIFACTS, List.of()),
                            Map.entry(AgentGraphStateKeys.FINAL_ANSWER, "")
                    ))
                    .build());

            when(modelClient.generate(any()))
                    .thenReturn(Mono.just(new ModelResponse("resume synthesis")));

            List<AgentEvent> events = graphResearchRuntime.run(request(runId, threadId,
                            new LoopConfig(1, 2, 30_000, ResearchOptions.quick())))
                    .collectList()
                    .block();

            assertThat(events).noneSatisfy(event ->
                    assertThat(event.type()).isEqualTo(AgentEventType.RESEARCH_PLAN_CREATED));
            assertThat(events).anySatisfy(event ->
                    assertThat(event.type()).isEqualTo(AgentEventType.SOURCE_FOUND));
            assertThat(planStore.findByRunId(runId)).hasValueSatisfying(plan ->
                    assertThat(plan.planId()).isEqualTo(existingPlan.planId()));

            Checkpoint finalCheckpoint = checkpointSaver.get(runnableConfig).orElseThrow();
            assertThat(finalCheckpoint.getNextNodeId()).isBlank();
            List<Map<String, Object>> modelSteps = (List<Map<String, Object>>) finalCheckpoint.getState()
                    .get(AgentGraphStateKeys.MODEL_STEPS);
            assertThat(modelSteps)
                    .noneSatisfy(step -> assertThat(step)
                            .containsEntry("node", "create_or_load_plan")
                            .containsEntry("status", "reused"));
        }
        finally {
            properties.getGraph().getCheckpoint().setEnabled(originalCheckpointEnabled);
        }
    }

    private static GraphResearchRuntimeRequest request(String runId, String threadId, LoopConfig loopConfig) {
        AgentRunConfig runConfig = new AgentRunConfig(
                threadId,
                runId,
                "zhipu",
                true,
                false,
                10,
                Path.of("."),
                RunMode.RESEARCH,
                loopConfig.researchOptions(),
                Map.of()
        );

        AgentRequest agentRequest = new AgentRequest(threadId, "deepseek technology software", "zhipu");
        AgentLoop loop = new AgentLoop(
                prompt -> Mono.just(new ModelResponse("research done")),
                new ToolRegistry(List.of())
        );

        return new GraphResearchRuntimeRequest(
                loop,
                loopConfig,
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
    }

    private static ResearchPlan plan(String runId, String threadId, String topic) {
        List<ResearchDimension> dimensions = List.of(
                new ResearchDimension("facts", "Facts and data", "Collect factual evidence",
                        ResearchTaskStatus.PENDING, List.of(topic + " facts"), 1, 0, 0, List.of()),
                new ResearchDimension("cases", "Case studies", "Collect examples",
                        ResearchTaskStatus.PENDING, List.of(topic + " case studies"), 1, 0, 0, List.of()),
                new ResearchDimension("limits", "Limitations and counter views", "Collect risks and counterpoints",
                        ResearchTaskStatus.PENDING, List.of(topic + " limitations"), 1, 0, 0, List.of())
        );
        return new ResearchPlan(
                "plan-" + UUID.randomUUID(),
                threadId,
                runId,
                topic,
                List.of("What matters about " + topic + "?"),
                dimensions,
                List.of(topic),
                "Prefer reliable sources",
                "Answer",
                "CREATED",
                Instant.now(),
                Instant.now()
        );
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
