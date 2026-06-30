package org.wrj.haifa.ai.deerflow.research;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

import static org.assertj.core.api.Assertions.assertThat;

class SourceRegistryTest {

    @Test
    void normalizesAndDeduplicatesEquivalentUrls() {
        InMemorySourceRegistry registry = new InMemorySourceRegistry(new SourceQualityScorer(), new DedupService());

        SourceCandidateRegistration first = registry.registerCandidate(
                "thread-1",
                "run-1",
                new SearchResultCandidate("Example", "http://Example.com/path/?utm_source=test&b=2&a=1#section", "snippet")
        );
        SourceCandidateRegistration second = registry.registerCandidate(
                "thread-1",
                "run-1",
                new SearchResultCandidate("Example", "https://example.com/path?a=1&b=2", "snippet")
        );

        assertThat(first.source().url()).isEqualTo("https://example.com/path?a=1&b=2");
        assertThat(second.deduplicated()).isTrue();
        assertThat(second.source().sourceId()).isEqualTo(first.source().sourceId());
        assertThat(registry.listByRun("run-1")).hasSize(1);
    }

    @Test
    void deduplicatesFetchedSourcesByContentHash() {
        InMemorySourceRegistry registry = new InMemorySourceRegistry(new SourceQualityScorer(), new DedupService());
        registry.registerCandidate(
                "thread-1",
                "run-1",
                new SearchResultCandidate("Mirror candidate", "https://mirror.example.com/copied-article", "mirror")
        );
        ExtractedWebContent extracted = new ExtractedWebContent(
                "Article",
                "This is the same body text for both pages and should deduplicate strongly.",
                "Author",
                null,
                "https://example.com/article",
                java.util.List.of(),
                java.util.List.of("title", "mainText"),
                java.util.List.of()
        );

        FetchRegistrationResult first = registry.registerFetched(
                "thread-1", "run-1", "https://example.com/article", extracted.mainText(), extracted,
                ResearchSourceType.WEB_PAGE, 0.82
        );
        FetchRegistrationResult second = registry.registerFetched(
                "thread-1", "run-1", "https://mirror.example.com/copied-article", extracted.mainText(), extracted,
                ResearchSourceType.WEB_PAGE, 0.41
        );

        assertThat(second.cached()).isTrue();
        assertThat(second.deduplicatedByContentHash()).isTrue();
        assertThat(second.stored().source().sourceId()).isEqualTo(first.stored().source().sourceId());
        assertThat(registry.listByRun("run-1")).hasSize(1);
    }
}
