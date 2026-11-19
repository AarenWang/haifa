package org.wrj.haifa.ai.deerflow.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.WebContentExtractor;

@Component
public class ResearchRuntimeSupport {

    private final SourceRegistry sourceRegistry;
    private final EvidenceStore evidenceStore;
    private final CitationManager citationManager;
    private final SourceQualityScorer scorer;
    private final SearchResultParser searchResultParser;
    private final WebContentExtractor webContentExtractor;
    private final EvidenceExtractor evidenceExtractor;
    private final DedupService dedupService;

    public ResearchRuntimeSupport(SourceRegistry sourceRegistry, EvidenceStore evidenceStore,
            CitationManager citationManager, SourceQualityScorer scorer,
            SearchResultParser searchResultParser, WebContentExtractor webContentExtractor,
            EvidenceExtractor evidenceExtractor, DedupService dedupService) {
        this.sourceRegistry = sourceRegistry;
        this.evidenceStore = evidenceStore;
        this.citationManager = citationManager;
        this.scorer = scorer;
        this.searchResultParser = searchResultParser;
        this.webContentExtractor = webContentExtractor;
        this.evidenceExtractor = evidenceExtractor;
        this.dedupService = dedupService;
    }

    public Optional<RegisteredSourceContent> reuseFetched(String url) {
        return this.sourceRegistry.findFetchedByUrl(url);
    }

    public SearchIngestionResult ingestSearchResults(String threadId, String runId, String rawResult) {
        List<SourceCandidateRegistration> registrations = new ArrayList<>();
        for (SearchResultCandidate candidate : this.searchResultParser.parse(rawResult)) {
            registrations.add(this.sourceRegistry.registerCandidate(threadId, runId, candidate));
        }
        String observation = registrations.isEmpty()
                ? ""
                : registrations.stream()
                .map(registration -> "- " + registration.source().title() + " (" + registration.source().domain() + ") "
                        + this.citationManager.inlineCitation(registration.source()))
                .collect(Collectors.joining("\n", "Registered source candidates:\n", ""));
        return new SearchIngestionResult(registrations, observation);
    }

    public FetchProcessingResult ingestFetchedContent(String threadId, String runId, String url, String rawContent) {
        var extracted = this.webContentExtractor.extract(url, rawContent);
        String contentHash = this.dedupService.contentHash(extracted.mainText());
        boolean duplicateContent = this.sourceRegistry.findFetchedByUrl(url)
                .map(existing -> StringUtils.hasText(existing.source().contentHash())
                        && existing.source().contentHash().equals(contentHash))
                .orElse(false);
        double credibility = this.scorer.scoreFetched(url, extracted, duplicateContent);
        FetchRegistrationResult registration = this.sourceRegistry.registerFetched(
                threadId, runId, url, rawContent, extracted, ResearchSourceType.WEB_PAGE, credibility);
        List<EvidenceItem> evidenceItems = new ArrayList<>();
        for (EvidenceItem evidenceItem : this.evidenceExtractor.extract(threadId, runId, registration.stored().source(), extracted)) {
            evidenceItems.add(this.evidenceStore.save(evidenceItem));
        }
        String observation = buildObservation(registration, evidenceItems);
        return new FetchProcessingResult(registration, evidenceItems, observation);
    }

    public CitationProcessingResult finalizeAnswer(String threadId, String runId, String answer) {
        return this.citationManager.finalizeAnswer(threadId, runId, answer);
    }

    public List<ResearchSource> listSourcesByRun(String runId) {
        return this.sourceRegistry.listByRun(runId);
    }

    public List<EvidenceItem> listEvidenceByRun(String runId) {
        return this.evidenceStore.listByRun(runId);
    }

    public int citationCount(String runId, String sourceId) {
        return this.citationManager.countByRunAndSource(runId, sourceId);
    }

    private String buildObservation(FetchRegistrationResult registration, List<EvidenceItem> evidenceItems) {
        ResearchSource source = registration.stored().source();
        StringBuilder builder = new StringBuilder();
        builder.append("Registered source ")
                .append(source.sourceId())
                .append(": ")
                .append(StringUtils.hasText(source.title()) ? source.title() : source.url())
                .append(" ")
                .append(this.citationManager.inlineCitation(source))
                .append('\n');
        if (!registration.stored().extractedContent().reliableFields().isEmpty()) {
            builder.append("Reliable fields: ")
                    .append(String.join(", ", registration.stored().extractedContent().reliableFields()))
                    .append('\n');
        }
        if (!evidenceItems.isEmpty()) {
            builder.append("Evidence extracted:\n");
            for (EvidenceItem evidenceItem : evidenceItems) {
                builder.append("- [").append(evidenceItem.evidenceId()).append("] ")
                        .append(evidenceItem.claim())
                        .append('\n');
            }
        }
        return builder.toString().trim();
    }
}
