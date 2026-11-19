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
import org.wrj.haifa.ai.deerflow.research.CitationManager;
import org.wrj.haifa.ai.deerflow.research.EvidenceExtractor;
import org.wrj.haifa.ai.deerflow.research.InMemoryEvidenceStore;
import org.wrj.haifa.ai.deerflow.research.InMemorySourceRegistry;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.SourceQualityScorer;
import org.wrj.haifa.ai.deerflow.research.SearchResultParser;
import org.wrj.haifa.ai.deerflow.research.plan.InMemoryResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchQualityGate;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.WebContentExtractor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class AgentLoopResearchIntegrationTest {

    @Test
    void registersSourcesExtractsEvidenceAndReusesFetchedContent() {
        InMemorySourceRegistry sourceRegistry = new InMemorySourceRegistry(new SourceQualityScorer(), new DedupService());
        InMemoryEvidenceStore evidenceStore = new InMemoryEvidenceStore();
        CitationManager citationManager = new CitationManager(sourceRegistry, evidenceStore);
        ResearchRuntimeSupport support = new ResearchRuntimeSupport(
                sourceRegistry,
                evidenceStore,
                citationManager,
                new SourceQualityScorer(),
                new SearchResultParser(),
                new WebContentExtractor(),
                new EvidenceExtractor(),
                new DedupService()
        );

        CountingFetchTool fetchTool = new CountingFetchTool();
        AgentTool searchTool = new FixedSearchTool();
        AgentModelClient modelClient = new ResearchModel();
        AgentLoop loop = new AgentLoop(modelClient, new ToolRegistry(List.of(searchTool, fetchTool)), null, null, null,
                new org.wrj.haifa.ai.deerflow.research.ResearchLoopObserver(support, null, null, null, null));

        AgentRunConfig config = new AgentRunConfig(
                "thread-r",
                "run-r",
                "test-model",
                false,
                false,
                6,
                Path.of("."),
                RunMode.RESEARCH,
                ResearchOptions.defaults(),
                Map.of()
        );

        List<AgentEvent> events = loop.run(
                new LoopConfig(6, 6, 60_000, ResearchOptions.defaults()),
                config,
                "Research system prompt",
                "Investigate source reuse",
                new AtomicInteger(),
                null,
                List.of(),
                List.of()
        ).collectList().block();

        assertThat(fetchTool.calls).isEqualTo(1);
        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.SOURCE_FOUND, AgentEventType.SOURCE_FETCHED,
                        AgentEventType.EVIDENCE_EXTRACTED, AgentEventType.RUN_COMPLETED);
        assertThat(sourceRegistry.listByRun("run-r")).isNotEmpty();
        assertThat(evidenceStore.listByRun("run-r")).allSatisfy(evidence ->
                assertThat(evidence.sourceId()).isNotBlank());
    }

    @Test
    void keepsResearchingUntilPlanAndQualityThresholdsAreSatisfied() {
        InMemorySourceRegistry sourceRegistry = new InMemorySourceRegistry(new SourceQualityScorer(), new DedupService());
        InMemoryEvidenceStore evidenceStore = new InMemoryEvidenceStore();
        CitationManager citationManager = new CitationManager(sourceRegistry, evidenceStore);
        ResearchRuntimeSupport support = new ResearchRuntimeSupport(
                sourceRegistry,
                evidenceStore,
                citationManager,
                new SourceQualityScorer(),
                new SearchResultParser(),
                new WebContentExtractor(),
                new EvidenceExtractor(),
                new DedupService()
        );
        InMemoryResearchPlanStore planStore = new InMemoryResearchPlanStore();
        ResearchProgressTracker progressTracker = new ResearchProgressTracker(planStore);
        ResearchQualityGate qualityGate = new ResearchQualityGate();

        ResearchPlan plan = new ResearchPlan(
                "plan-qg",
                "thread-qg",
                "run-qg",
                "AI chip market analysis",
                List.of("What are the key trends?"),
                List.of(
                        new ResearchDimension("d1", "Overview & Data", "desc", ResearchTaskStatus.IN_PROGRESS, List.of("ai chips market size"), 2, 0, 0, List.of()),
                        new ResearchDimension("d2", "Case Studies", "desc", ResearchTaskStatus.PENDING, List.of("ai chips company case study"), 2, 0, 0, List.of()),
                        new ResearchDimension("d3", "Risks & Counterpoints", "desc", ResearchTaskStatus.PENDING, List.of("ai chips supply chain risks"), 1, 0, 0, List.of())
                ),
                List.of("ai chips market size", "ai chips company case study", "ai chips supply chain risks"),
                "Prefer authoritative sources",
                "A cited answer",
                "IN_PROGRESS",
                Instant.now(),
                Instant.now()
        );
        planStore.save(plan);
        progressTracker.syncTasksFromPlan("run-qg");

        AgentLoop loop = new AgentLoop(
                new SequencedResearchModel(),
                new ToolRegistry(List.of(new MultiAngleSearchTool(), new MultiPageFetchTool())),
                null,
                null,
                null,
                new org.wrj.haifa.ai.deerflow.research.ResearchLoopObserver(support, null, planStore, progressTracker, qualityGate)
        );

        AgentRunConfig config = new AgentRunConfig(
                "thread-qg",
                "run-qg",
                "test-model",
                false,
                false,
                10,
                Path.of("."),
                RunMode.RESEARCH,
                ResearchOptions.standard(),
                Map.of()
        );

        List<AgentEvent> events = loop.run(
                new LoopConfig(10, 10, 60_000, ResearchOptions.standard()),
                config,
                "Research system prompt",
                "Analyze the AI chip market",
                new AtomicInteger(),
                null,
                List.of(),
                List.of()
        ).collectList().block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.RESEARCH_DIMENSION_COMPLETED, AgentEventType.RUN_COMPLETED, AgentEventType.MODEL_COMPLETED);
        assertThat(events.stream().filter(event -> event.type() == AgentEventType.TOOL_CALL_REQUESTED).count()).isGreaterThanOrEqualTo(8);
        assertThat(events.stream().filter(event -> event.type() == AgentEventType.SOURCE_FETCHED).count()).isEqualTo(5);
        assertThat(events.stream()
                .filter(event -> event.type() == AgentEventType.MODEL_COMPLETED)
                .reduce((first, second) -> second)
                .orElseThrow()
                .content()).contains("Limitations:");
        assertThat(planStore.findByRunId("run-qg")).isPresent();
        assertThat(planStore.findByRunId("run-qg").orElseThrow().completedDimensionCount()).isEqualTo(3);
    }

    @Test
    void researchWithoutLegacyPlanAcceptsSkillFinalAnswer() {
        AgentLoop loop = new AgentLoop(prompt -> Mono.just(new ModelResponse("premature report")),
                new ToolRegistry(List.of()), null, null, null,
                new org.wrj.haifa.ai.deerflow.research.ResearchLoopObserver(null, null, null, null, null));

        AgentRunConfig config = new AgentRunConfig(
                "thread-noplan",
                "run-noplan",
                "test-model",
                false,
                false,
                2,
                Path.of("."),
                RunMode.RESEARCH,
                ResearchOptions.standard(),
                Map.of()
        );

        List<AgentEvent> events = loop.run(
                new LoopConfig(2, 2, 60_000, ResearchOptions.standard()),
                config,
                "Research system prompt",
                "Write me a deep research report immediately",
                new AtomicInteger(),
                null,
                List.of(),
                List.of()
        ).collectList().block();

        assertThat(events).extracting(AgentEvent::type).contains(AgentEventType.MODEL_COMPLETED, AgentEventType.RUN_COMPLETED);
        assertThat(events).extracting(AgentEvent::content).contains("premature report");
    }

    private static final class FixedSearchTool implements AgentTool {

        @Override
        public String name() {
            return "web_search";
        }

        @Override
        public String description() {
            return "Fixed web search";
        }

        @Override
        public boolean supports(String userMessage) {
            return true;
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            return ToolResult.of(name(), """
                    Search results for: reuse

                    1. [Example Article] https://example.com/article?utm_source=test
                       Summary: Core article for the test.

                    2. [Example Article Mirror] https://example.com/article
                       Summary: Same article in a different URL form.
                    """);
        }
    }

    private static final class MultiAngleSearchTool implements AgentTool {
        @Override
        public String name() {
            return "web_search";
        }

        @Override
        public String description() {
            return "Returns different sources per query.";
        }

        @Override
        public boolean supports(String userMessage) {
            return true;
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            String query = request.userMessage();
            if (query.contains("market-size")) {
                return ToolResult.of(name(), """
                        1. [Market Data] https://example.com/market-data
                           Summary: Market size statistics.

                        2. [Market Forecast] https://example.com/market-forecast
                           Summary: Forecast data.
                        """);
            }
            if (query.contains("case-study")) {
                return ToolResult.of(name(), """
                        1. [Case Study] https://example.com/case-study
                           Summary: Adoption example.

                        2. [Expert View] https://example.com/expert-view
                           Summary: Expert commentary.
                        """);
            }
            return ToolResult.of(name(), """
                    1. [Risks] https://example.com/risks
                       Summary: Supply chain and policy risks.
                    """);
        }
    }

    private static final class MultiPageFetchTool implements AgentTool {
        @Override
        public String name() {
            return "web_fetch";
        }

        @Override
        public String description() {
            return "Fetches multiple long-form pages.";
        }

        @Override
        public boolean supports(String userMessage) {
            return true;
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            String url = request.userMessage().replace("{\"url\":\"", "").replace("\"}", "");
            String body = switch (url) {
                case "https://example.com/market-data" -> """
                        <html><body><article>
                        According to the latest market data, AI chip revenue reached 42 billion dollars in 2025, providing a factual baseline for the category.
                        Analysts reported that the number represents a 28 percent year-over-year increase, which adds concrete data for the growth narrative.
                        The report suggests sustained enterprise demand across training and inference workloads, which supports the broader overview dimension.
                        </article></body></html>
                        """;
                case "https://example.com/market-forecast" -> """
                        <html><body><article>
                        Forecast models project the market could exceed 60 billion dollars by 2027, and the publication explains the assumptions behind that estimate.
                        The article includes comparative numbers across hyperscalers and edge vendors, which expands the quantitative evidence base.
                        It also notes that optimistic scenarios depend on continued packaging capacity, introducing an early limitation to the forecast.
                        </article></body></html>
                        """;
                case "https://example.com/case-study" -> """
                        <html><body><article>
                        This case study describes how a cloud provider deployed custom accelerators to reduce inference cost in production by double-digit percentages.
                        The example shows that the rollout required changes in compiler tooling and operational readiness, making it a concrete implementation case.
                        Project leaders said the deployment succeeded because the platform team aligned hardware, software, and scheduling controls from the outset.
                        </article></body></html>
                        """;
                case "https://example.com/expert-view" -> """
                        <html><body><article>
                        An industry expert said the next phase of adoption will depend on software portability rather than raw peak throughput alone.
                        The analyst commentary compares vendor strategies and highlights where ecosystem investments may outweigh silicon benchmarks.
                        The piece also argues that some enterprise buyers will prefer flexible general-purpose GPUs, providing a counterpoint to custom-chip enthusiasm.
                        </article></body></html>
                        """;
                default -> """
                        <html><body><article>
                        Risk analysis reported that supply chain concentration, export controls, and energy constraints remain meaningful challenges for AI chip vendors.
                        Critics argued that capital intensity and procurement delays could slow adoption, which provides an opposing viewpoint that tempers bullish forecasts.
                        The report concludes that these limitations must be stated explicitly whenever growth projections are summarized for decision-makers.
                        </article></body></html>
                        """;
            };
            return ToolResult.of(name(), body, Map.of("url", url));
        }
    }

    private static final class CountingFetchTool implements AgentTool {
        private int calls = 0;

        @Override
        public String name() {
            return "web_fetch";
        }

        @Override
        public String description() {
            return "Counting fetch";
        }

        @Override
        public boolean supports(String userMessage) {
            return true;
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            this.calls++;
            String url = request.userMessage().contains("https://example.com/article")
                    ? "https://example.com/article"
                    : "https://example.com/article";
            String html = """
                    <html>
                      <head>
                        <title>Example Article</title>
                        <link rel="canonical" href="https://example.com/article" />
                      </head>
                      <body>
                        <nav>Home Cookie Banner</nav>
                        <article>
                          <p>Research loops should reuse fetched pages when the same canonical URL appears again.</p>
                          <p>Evidence must remain traceable to the source that produced it.</p>
                        </article>
                        <footer>Privacy</footer>
                      </body>
                    </html>
                    """;
            return ToolResult.of(name(), html, Map.of("url", url));
        }
    }

    private static final class ResearchModel implements AgentModelClient {
        private int callCount = 0;

        @Override
        public Mono<ModelResponse> generate(ModelPrompt prompt) {
            this.callCount++;
            return switch (this.callCount) {
                case 1 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-reuse-search", "web_search", "{\"query\":\"reuse\"}"))));
                case 2 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-reuse-fetch-1", "web_fetch", "{\"url\":\"https://example.com/article?utm_source=test\"}"))));
                case 3 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-reuse-fetch-2", "web_fetch", "{\"url\":\"https://example.com/article\"}"))));
                default -> Mono.just(new ModelResponse(
                        "Canonical URLs should only be fetched once [citation:Example Article](https://example.com/article)"
                ));
            };
        }
    }

    private static final class SequencedResearchModel implements AgentModelClient {
        private int callCount = 0;

        @Override
        public Mono<ModelResponse> generate(ModelPrompt prompt) {
            this.callCount++;
            return switch (this.callCount) {
                case 1 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-market-search", "web_search", "{\"query\":\"market-size\"}"))));
                case 2 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-market-data", "web_fetch", "{\"url\":\"https://example.com/market-data\"}"))));
                case 3 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-market-forecast", "web_fetch", "{\"url\":\"https://example.com/market-forecast\"}"))));
                case 4 -> Mono.just(new ModelResponse("Here is an early summary."));
                case 5 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-case-search", "web_search", "{\"query\":\"case-study\"}"))));
                case 6 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-case-fetch", "web_fetch", "{\"url\":\"https://example.com/case-study\"}"))));
                case 7 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-expert-fetch", "web_fetch", "{\"url\":\"https://example.com/expert-view\"}"))));
                case 8 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-risks-search", "web_search", "{\"query\":\"risks\"}"))));
                case 9 -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-risks-fetch", "web_fetch", "{\"url\":\"https://example.com/risks\"}"))));
                default -> Mono.just(new ModelResponse(
                        "The AI chip market is growing quickly [citation:Market Data](https://example.com/market-data)"
                ));
            };
        }
    }
}

