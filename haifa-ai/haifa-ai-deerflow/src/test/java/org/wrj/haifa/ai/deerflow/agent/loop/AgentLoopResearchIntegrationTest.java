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
import org.wrj.haifa.ai.deerflow.research.CitationManager;
import org.wrj.haifa.ai.deerflow.research.EvidenceExtractor;
import org.wrj.haifa.ai.deerflow.research.InMemoryEvidenceStore;
import org.wrj.haifa.ai.deerflow.research.InMemorySourceRegistry;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.SourceQualityScorer;
import org.wrj.haifa.ai.deerflow.research.SearchResultParser;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.WebContentExtractor;
import reactor.core.publisher.Mono;

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
        AgentLoop loop = new AgentLoop(modelClient, new ToolRegistry(List.of(searchTool, fetchTool)), null, null, null, support);

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
                        AgentEventType.EVIDENCE_EXTRACTED, AgentEventType.MODEL_COMPLETED);
        assertThat(sourceRegistry.listByRun("run-r")).isNotEmpty();
        assertThat(evidenceStore.listByRun("run-r")).allSatisfy(evidence ->
                assertThat(evidence.sourceId()).isNotBlank());
        assertThat(events.stream()
                .filter(event -> event.type() == AgentEventType.MODEL_COMPLETED)
                .findFirst()
                .orElseThrow()
                .content()).contains("Sources");
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
                case 1 -> Mono.just(new ModelResponse("<tool_call name=\"web_search\">{\"query\":\"reuse\"}</tool_call>"));
                case 2 -> Mono.just(new ModelResponse("<tool_call name=\"web_fetch\">{\"url\":\"https://example.com/article?utm_source=test\"}</tool_call>"));
                case 3 -> Mono.just(new ModelResponse("<tool_call name=\"web_fetch\">{\"url\":\"https://example.com/article\"}</tool_call>"));
                default -> Mono.just(new ModelResponse(
                        "<final_answer>Canonical URLs should only be fetched once [citation:Example Article](https://example.com/article)</final_answer>"
                ));
            };
        }
    }
}
