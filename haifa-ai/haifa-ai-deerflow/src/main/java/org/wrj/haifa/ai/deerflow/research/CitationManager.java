package org.wrj.haifa.ai.deerflow.research;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CitationManager {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[citation:([^\\]]+)]\\((https?://[^)]+)\\)");

    private final SourceRegistry sourceRegistry;
    private final EvidenceStore evidenceStore;
    private final Map<String, CitationRecord> citationsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> citationIdsByRun = new ConcurrentHashMap<>();

    public CitationManager(SourceRegistry sourceRegistry, EvidenceStore evidenceStore) {
        this.sourceRegistry = sourceRegistry;
        this.evidenceStore = evidenceStore;
    }

    public String inlineCitation(ResearchSource source) {
        String title = StringUtils.hasText(source.title()) ? source.title() : source.domain();
        return "[citation:" + title + "](" + source.url() + ")";
    }

    public CitationProcessingResult finalizeAnswer(String threadId, String runId, String answer) {
        if (!StringUtils.hasText(answer)) {
            return new CitationProcessingResult("", List.of(), List.of());
        }
        List<CitationRecord> records = new ArrayList<>();
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            String title = matcher.group(1);
            String url = matcher.group(2);
            String claim = extractClaim(answer, matcher.start());
            resolveSource(title, url, runId).ifPresent(source -> {
                List<String> evidenceIds = resolveEvidenceIds(source, claim);
                CitationRecord record = new CitationRecord(
                        UUID.randomUUID().toString(),
                        threadId,
                        runId,
                        source.sourceId(),
                        evidenceIds,
                        claim,
                        matcher.group(),
                        Instant.now()
                );
                this.citationsById.put(record.citationId(), record);
                this.citationIdsByRun.computeIfAbsent(runId, ignored -> new ArrayList<>()).add(record.citationId());
                records.add(record);
            });
        }

        Map<String, ResearchSource> citedSources = new LinkedHashMap<>();
        for (CitationRecord record : records) {
            this.sourceRegistry.findBySourceId(record.sourceId()).ifPresent(source ->
                    citedSources.putIfAbsent(source.sourceId(), source));
        }

        String finalAnswer = answer;
        if (!citedSources.isEmpty() && !containsSourcesSection(answer)) {
            StringBuilder builder = new StringBuilder(answer.trim());
            builder.append("\n\nSources\n");
            citedSources.values().stream()
                    .sorted(Comparator.comparing(ResearchSource::title))
                    .forEach(source -> builder.append("- [")
                            .append(StringUtils.hasText(source.title()) ? source.title() : source.domain())
                            .append("](").append(source.url()).append(")")
                            .append(" - ").append(source.domain())
                            .append('\n'));
            finalAnswer = builder.toString().trim();
        }

        return new CitationProcessingResult(finalAnswer, records, List.copyOf(citedSources.values()));
    }

    public List<CitationRecord> listByRun(String runId) {
        List<String> ids = this.citationIdsByRun.getOrDefault(runId, List.of());
        List<CitationRecord> records = new ArrayList<>();
        for (String id : ids) {
            CitationRecord record = this.citationsById.get(id);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    public int countByRunAndSource(String runId, String sourceId) {
        int count = 0;
        for (CitationRecord record : listByRun(runId)) {
            if (sourceId.equals(record.sourceId())) {
                count++;
            }
        }
        return count;
    }

    private Optional<ResearchSource> resolveSource(String title, String url, String runId) {
        Optional<ResearchSource> byUrl = this.sourceRegistry.findByUrl(url);
        if (byUrl.isPresent()) {
            return byUrl;
        }
        String normalizedTitle = normalize(title);
        return this.sourceRegistry.listByRun(runId).stream()
                .filter(source -> normalize(source.title()).equals(normalizedTitle))
                .findFirst();
    }

    private List<String> resolveEvidenceIds(ResearchSource source, String claim) {
        List<EvidenceItem> evidenceItems = this.evidenceStore.listBySourceId(source.sourceId());
        if (evidenceItems.isEmpty()) {
            return List.of();
        }
        String normalizedClaim = normalize(claim);
        return evidenceItems.stream()
                .sorted(Comparator.<EvidenceItem>comparingInt(item ->
                        overlapScore(normalizedClaim, normalize(item.claim()))).reversed())
                .limit(1)
                .map(EvidenceItem::evidenceId)
                .toList();
    }

    private static int overlapScore(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0;
        }
        int score = 0;
        for (String token : left.split(" ")) {
            if (token.length() > 3 && right.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private static boolean containsSourcesSection(String answer) {
        String lower = answer.toLowerCase(Locale.ROOT);
        return lower.contains("\nsources\n") || lower.contains("\n## sources");
    }

    private static String extractClaim(String answer, int citationStart) {
        String prefix = answer.substring(0, citationStart).trim();
        int split = Math.max(prefix.lastIndexOf('.'), Math.max(prefix.lastIndexOf('\n'), prefix.lastIndexOf('!')));
        return split >= 0 ? prefix.substring(split + 1).trim() : prefix;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unused")
    private static String domainOf(String url) {
        try {
            return Optional.ofNullable(URI.create(url).getHost()).orElse("");
        }
        catch (Exception ex) {
            return "";
        }
    }
}
