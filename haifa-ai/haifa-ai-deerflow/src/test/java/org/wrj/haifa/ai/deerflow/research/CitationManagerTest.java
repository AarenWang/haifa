package org.wrj.haifa.ai.deerflow.research;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

import static org.assertj.core.api.Assertions.assertThat;

class CitationManagerTest {

    @Test
    void finalizesAnswerWithOnlyReferencedSourcesAndBacklinksToEvidence() {
        InMemorySourceRegistry sourceRegistry = new InMemorySourceRegistry(new SourceQualityScorer(), new DedupService());
        InMemoryEvidenceStore evidenceStore = new InMemoryEvidenceStore();
        CitationManager citationManager = new CitationManager(sourceRegistry, evidenceStore);

        ExtractedWebContent extracted = new ExtractedWebContent(
                "Official Docs",
                "Spring AI integrates tool calling and structured prompts for agent systems.",
                "Team",
                Instant.parse("2026-01-01T00:00:00Z"),
                "https://docs.example.com/spring-ai",
                java.util.List.of(),
                java.util.List.of("title", "mainText", "canonicalUrl"),
                java.util.List.of()
        );
        FetchRegistrationResult docsSource = sourceRegistry.registerFetched(
                "thread-1", "run-1", "https://docs.example.com/spring-ai", extracted.mainText(), extracted,
                ResearchSourceType.DOCUMENTATION, 0.92
        );
        sourceRegistry.registerCandidate("thread-1", "run-1",
                new SearchResultCandidate("Unused source", "https://news.example.com/article", "unused"));

        EvidenceItem evidence = evidenceStore.save(new EvidenceItem(
                "e-1", "thread-1", "run-1", docsSource.stored().source().sourceId(),
                "Spring AI integrates tool calling and structured prompts for agent systems.",
                "Spring AI integrates tool calling and structured prompts for agent systems.",
                "overview", 0.91, Instant.now()
        ));

        CitationProcessingResult result = citationManager.finalizeAnswer(
                "thread-1",
                "run-1",
                "Spring AI can orchestrate tool use effectively "
                        + citationManager.inlineCitation(docsSource.stored().source())
        );

        assertThat(result.citedSources()).hasSize(1);
        assertThat(result.citedSources().get(0).sourceId()).isEqualTo(docsSource.stored().source().sourceId());
        assertThat(result.finalAnswer()).contains("Sources");
        assertThat(result.finalAnswer()).doesNotContain("Unused source");
        assertThat(result.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.evidenceIds()).containsExactly(evidence.evidenceId());
            assertThat(citation.sourceId()).isEqualTo(docsSource.stored().source().sourceId());
        });
        assertThat(citationManager.countByRunAndSource("run-1", docsSource.stored().source().sourceId())).isEqualTo(1);
    }
}
