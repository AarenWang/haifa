package org.wrj.haifa.ai.deerflow.research;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

@Component
public class InMemorySourceRegistry implements SourceRegistry {

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "gclid", "fbclid", "mc_cid", "mc_eid", "ref", "ref_src", "s_cid"
    );

    private final SourceQualityScorer scorer;
    private final DedupService dedupService;
    private final Map<String, ResearchSource> sourcesById = new ConcurrentHashMap<>();
    private final Map<String, String> sourceIdByNormalizedUrl = new ConcurrentHashMap<>();
    private final Map<String, String> sourceIdByContentHash = new ConcurrentHashMap<>();
    private final Map<String, RegisteredSourceContent> fetchedContentBySourceId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sourceIdsByThread = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sourceIdsByRun = new ConcurrentHashMap<>();

    public InMemorySourceRegistry(SourceQualityScorer scorer, DedupService dedupService) {
        this.scorer = scorer;
        this.dedupService = dedupService;
    }

    @Override
    public String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        try {
            URI original = URI.create(url.trim());
            String scheme = "https";
            String host = Optional.ofNullable(original.getHost())
                    .orElseGet(() -> extractHostFromAuthority(original.getAuthority()))
                    .toLowerCase(Locale.ROOT);
            int port = original.getPort();
            String path = Optional.ofNullable(original.getPath()).orElse("");
            path = path.replaceAll("/{2,}", "/");
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            String query = normalizeQuery(original.getRawQuery());
            URI normalized = new URI(
                    scheme,
                    null,
                    host,
                    isDefaultPort(port) ? -1 : port,
                    path.isBlank() ? "/" : path,
                    query,
                    null
            );
            return normalized.toString();
        }
        catch (Exception ex) {
            return url.trim();
        }
    }

    @Override
    public SourceCandidateRegistration registerCandidate(String threadId, String runId, SearchResultCandidate candidate) {
        String normalizedUrl = normalizeUrl(candidate.url());
        ResearchSource existing = findByNormalizedUrl(normalizedUrl).orElse(null);
        if (existing != null) {
            index(existing.sourceId(), threadId, runId);
            return new SourceCandidateRegistration(existing, true);
        }

        ResearchSource source = new ResearchSource(
                UUID.randomUUID().toString(),
                threadId,
                runId,
                candidate.title(),
                normalizedUrl,
                normalizedUrl,
                domainOf(normalizedUrl),
                null,
                null,
                ResearchSourceType.SEARCH_RESULT,
                this.scorer.scoreCandidate(normalizedUrl, candidate.title(), candidate.snippet()),
                candidate.snippet(),
                ""
        );
        this.sourcesById.put(source.sourceId(), source);
        this.sourceIdByNormalizedUrl.put(normalizedUrl, source.sourceId());
        index(source.sourceId(), threadId, runId);
        return new SourceCandidateRegistration(source, false);
    }

    @Override
    public FetchRegistrationResult registerFetched(String threadId, String runId, String url, String rawContent,
            ExtractedWebContent extractedContent, ResearchSourceType sourceType, double credibility) {
        String normalizedUrl = normalizeUrl(url);
        String canonicalUrl = normalizeUrl(StringUtils.hasText(extractedContent.canonicalUrl())
                ? extractedContent.canonicalUrl() : normalizedUrl);
        String contentHash = this.dedupService.contentHash(extractedContent.mainText());

        if (StringUtils.hasText(contentHash) && this.sourceIdByContentHash.containsKey(contentHash)) {
            String existingId = this.sourceIdByContentHash.get(contentHash);
            ResearchSource existingSource = this.sourcesById.get(existingId);
            RegisteredSourceContent existingContent = this.fetchedContentBySourceId.get(existingId);
            if (existingSource != null && existingContent != null) {
                String displacedSourceId = this.sourceIdByNormalizedUrl.get(normalizedUrl);
                this.sourceIdByNormalizedUrl.put(normalizedUrl, existingId);
                if (StringUtils.hasText(canonicalUrl)) {
                    this.sourceIdByNormalizedUrl.put(canonicalUrl, existingId);
                }
                replaceIndexedSource(displacedSourceId, existingId, threadId, runId);
                index(existingId, threadId, runId);
                return new FetchRegistrationResult(existingContent, true, false, true);
            }
        }

        String sourceId = this.sourceIdByNormalizedUrl.get(normalizedUrl);
        boolean deduplicatedByUrl = StringUtils.hasText(sourceId);
        ResearchSource source = deduplicatedByUrl ? this.sourcesById.get(sourceId) : null;
        if (source == null) {
            source = new ResearchSource(
                    UUID.randomUUID().toString(),
                    threadId,
                    runId,
                    extractedContent.title(),
                    normalizedUrl,
                    canonicalUrl,
                    domainOf(canonicalUrl.isBlank() ? normalizedUrl : canonicalUrl),
                    extractedContent.publishedAt(),
                    Instant.now(),
                    sourceType,
                    credibility,
                    snippetOf(extractedContent.mainText()),
                    contentHash
            );
        } else {
            source = source.withFetchState(
                    extractedContent.title(),
                    canonicalUrl,
                    extractedContent.publishedAt(),
                    Instant.now(),
                    sourceType,
                    credibility,
                    snippetOf(extractedContent.mainText()),
                    contentHash
            );
        }

        this.sourcesById.put(source.sourceId(), source);
        this.sourceIdByNormalizedUrl.put(normalizedUrl, source.sourceId());
        if (StringUtils.hasText(canonicalUrl)) {
            this.sourceIdByNormalizedUrl.put(canonicalUrl, source.sourceId());
        }
        if (StringUtils.hasText(contentHash)) {
            this.sourceIdByContentHash.put(contentHash, source.sourceId());
        }
        RegisteredSourceContent stored = new RegisteredSourceContent(source, rawContent, extractedContent);
        this.fetchedContentBySourceId.put(source.sourceId(), stored);
        index(source.sourceId(), threadId, runId);
        return new FetchRegistrationResult(stored, false, deduplicatedByUrl, false);
    }

    @Override
    public Optional<ResearchSource> findBySourceId(String sourceId) {
        return Optional.ofNullable(this.sourcesById.get(sourceId));
    }

    @Override
    public Optional<RegisteredSourceContent> findFetchedByUrl(String url) {
        return findByUrl(url).flatMap(source -> Optional.ofNullable(this.fetchedContentBySourceId.get(source.sourceId())));
    }

    @Override
    public Optional<ResearchSource> findByUrl(String url) {
        return findByNormalizedUrl(normalizeUrl(url));
    }

    @Override
    public List<ResearchSource> listByThread(String threadId) {
        return listSources(this.sourceIdsByThread.getOrDefault(threadId, Set.of()));
    }

    @Override
    public List<ResearchSource> listByRun(String runId) {
        return listSources(this.sourceIdsByRun.getOrDefault(runId, Set.of()));
    }

    private Optional<ResearchSource> findByNormalizedUrl(String normalizedUrl) {
        String sourceId = this.sourceIdByNormalizedUrl.get(normalizedUrl);
        return sourceId == null ? Optional.empty() : Optional.ofNullable(this.sourcesById.get(sourceId));
    }

    private void index(String sourceId, String threadId, String runId) {
        this.sourceIdsByThread.computeIfAbsent(threadId, ignored -> ConcurrentHashMap.newKeySet()).add(sourceId);
        this.sourceIdsByRun.computeIfAbsent(runId, ignored -> ConcurrentHashMap.newKeySet()).add(sourceId);
    }

    private void replaceIndexedSource(String displacedSourceId, String replacementSourceId, String threadId, String runId) {
        if (!StringUtils.hasText(displacedSourceId) || displacedSourceId.equals(replacementSourceId)) {
            return;
        }
        Set<String> threadSources = this.sourceIdsByThread.get(threadId);
        if (threadSources != null) {
            threadSources.remove(displacedSourceId);
        }
        Set<String> runSources = this.sourceIdsByRun.get(runId);
        if (runSources != null) {
            runSources.remove(displacedSourceId);
        }
    }

    private List<ResearchSource> listSources(Set<String> sourceIds) {
        List<ResearchSource> sources = new ArrayList<>();
        for (String sourceId : sourceIds) {
            ResearchSource source = this.sourcesById.get(sourceId);
            if (source != null) {
                sources.add(source);
            }
        }
        sources.sort(Comparator.comparing(ResearchSource::fetchedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchSource::title));
        return sources;
    }

    private static String snippetOf(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    private static String domainOf(String url) {
        try {
            return Optional.ofNullable(URI.create(url).getHost()).orElse("").toLowerCase(Locale.ROOT);
        }
        catch (Exception ex) {
            return "";
        }
    }

    private static String extractHostFromAuthority(String authority) {
        if (!StringUtils.hasText(authority)) {
            return "";
        }
        int atIndex = authority.indexOf('@');
        String hostPort = atIndex >= 0 ? authority.substring(atIndex + 1) : authority;
        int colonIndex = hostPort.indexOf(':');
        return colonIndex >= 0 ? hostPort.substring(0, colonIndex) : hostPort;
    }

    private static String normalizeQuery(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }
        return List.of(rawQuery.split("&")).stream()
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .map(part -> {
                    int idx = part.indexOf('=');
                    return idx < 0 ? new String[]{part, ""} : new String[]{part.substring(0, idx), part.substring(idx + 1)};
                })
                .filter(entry -> !TRACKING_PARAMS.contains(entry[0].toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(entry -> entry[0]))
                .map(entry -> entry[1].isBlank() ? entry[0] : entry[0] + "=" + entry[1])
                .collect(Collectors.joining("&"));
    }

    private static boolean isDefaultPort(int port) {
        return port == -1 || port == 80 || port == 443;
    }
}
