package org.wrj.haifa.ai.deerflow.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchSourceEntity;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchSourceMappingEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.ResearchSourceMappingRepository;
import org.wrj.haifa.ai.deerflow.persistence.repository.ResearchSourceRepository;
import org.wrj.haifa.ai.deerflow.webcontent.DedupService;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Primary
public class SQLiteSourceRegistry implements SourceRegistry {

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "gclid", "fbclid", "mc_cid", "mc_eid", "ref", "ref_src", "s_cid"
    );

    private final SourceQualityScorer scorer;
    private final DedupService dedupService;
    private final ResearchSourceRepository sourceRepository;
    private final ResearchSourceMappingRepository mappingRepository;
    private final ObjectMapper mapper;

    public SQLiteSourceRegistry(SourceQualityScorer scorer, DedupService dedupService,
                                ResearchSourceRepository sourceRepository,
                                ResearchSourceMappingRepository mappingRepository,
                                ObjectMapper mapper) {
        this.scorer = scorer;
        this.dedupService = dedupService;
        this.sourceRepository = sourceRepository;
        this.mappingRepository = mappingRepository;
        this.mapper = mapper;
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
    @Transactional
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
        saveSource(source, null, null, false);
        index(source.sourceId(), threadId, runId);
        return new SourceCandidateRegistration(source, false);
    }

    @Override
    @Transactional
    public FetchRegistrationResult registerFetched(String threadId, String runId, String url, String rawContent,
                                                   ExtractedWebContent extractedContent, ResearchSourceType sourceType, double credibility) {
        String normalizedUrl = normalizeUrl(url);
        String canonicalUrl = normalizeUrl(StringUtils.hasText(extractedContent.canonicalUrl())
                ? extractedContent.canonicalUrl() : normalizedUrl);
        String contentHash = this.dedupService.contentHash(extractedContent.mainText());

        if (StringUtils.hasText(contentHash)) {
            Optional<ResearchSourceEntity> existingEntityOpt = sourceRepository.findByContentHash(contentHash);
            if (existingEntityOpt.isPresent()) {
                ResearchSourceEntity existingEntity = existingEntityOpt.get();
                ResearchSource existingSource = deserializeSource(existingEntity);
                RegisteredSourceContent existingContent = toRegisteredSourceContent(existingEntity, existingSource);
                if (existingContent != null) {
                    String displacedSourceId = sourceRepository.findByNormalizedUrl(normalizedUrl)
                            .map(ResearchSourceEntity::getSourceId).orElse(null);
                    replaceIndexedSource(displacedSourceId, existingSource.sourceId(), threadId, runId);
                    index(existingSource.sourceId(), threadId, runId);
                    return new FetchRegistrationResult(existingContent, true, false, true);
                }
            }
        }

        Optional<ResearchSourceEntity> existingEntityOpt = sourceRepository.findByNormalizedUrl(normalizedUrl);
        boolean deduplicatedByUrl = existingEntityOpt.isPresent();
        ResearchSource source;
        if (!deduplicatedByUrl) {
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
            ResearchSource baseSource = deserializeSource(existingEntityOpt.get());
            source = baseSource.withFetchState(
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

        saveSource(source, rawContent, extractedContent, true);
        RegisteredSourceContent stored = new RegisteredSourceContent(source, rawContent, extractedContent);
        index(source.sourceId(), threadId, runId);
        return new FetchRegistrationResult(stored, false, deduplicatedByUrl, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResearchSource> findBySourceId(String sourceId) {
        if (sourceId == null) {
            return Optional.empty();
        }
        return sourceRepository.findById(sourceId).map(this::deserializeSource);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegisteredSourceContent> findFetchedByUrl(String url) {
        return findByUrl(url).flatMap(source -> sourceRepository.findById(source.sourceId())
                .map(entity -> toRegisteredSourceContent(entity, source)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResearchSource> findByUrl(String url) {
        return findByNormalizedUrl(normalizeUrl(url));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchSource> listByThread(String threadId) {
        if (threadId == null) {
            return List.of();
        }
        List<String> sourceIds = mappingRepository.findByThreadId(threadId).stream()
                .map(ResearchSourceMappingEntity::getSourceId)
                .distinct()
                .toList();
        return loadAndSortSources(sourceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchSource> listByRun(String runId) {
        if (runId == null) {
            return List.of();
        }
        List<String> sourceIds = mappingRepository.findByRunId(runId).stream()
                .map(ResearchSourceMappingEntity::getSourceId)
                .distinct()
                .toList();
        return loadAndSortSources(sourceIds);
    }

    private Optional<ResearchSource> findByNormalizedUrl(String normalizedUrl) {
        return sourceRepository.findByNormalizedUrl(normalizedUrl).map(this::deserializeSource);
    }

    private void index(String sourceId, String threadId, String runId) {
        if (mappingRepository.findBySourceIdAndThreadIdAndRunId(sourceId, threadId, runId).isEmpty()) {
            mappingRepository.save(new ResearchSourceMappingEntity(
                    UUID.randomUUID().toString(),
                    sourceId,
                    threadId,
                    runId
            ));
        }
    }

    private void replaceIndexedSource(String displacedSourceId, String replacementSourceId, String threadId, String runId) {
        if (displacedSourceId == null || displacedSourceId.equals(replacementSourceId)) {
            return;
        }
        mappingRepository.deleteBySourceIdAndThreadIdAndRunId(displacedSourceId, threadId, runId);
    }

    private List<ResearchSource> loadAndSortSources(List<String> sourceIds) {
        List<ResearchSource> sources = new ArrayList<>();
        for (String sourceId : sourceIds) {
            findBySourceId(sourceId).ifPresent(sources::add);
        }
        sources.sort(Comparator.comparing(ResearchSource::fetchedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchSource::title));
        return sources;
    }

    private void saveSource(ResearchSource source, String rawContent, ExtractedWebContent extractedContent, boolean fetched) {
        try {
            String fullSourceJson = mapper.writeValueAsString(source);
            String extractedJson = extractedContent != null ? mapper.writeValueAsString(extractedContent) : null;
            ResearchSourceEntity entity = new ResearchSourceEntity(
                    source.sourceId(),
                    source.threadId(),
                    source.runId(),
                    source.url(),
                    source.contentHash(),
                    fullSourceJson,
                    rawContent,
                    extractedJson,
                    fetched
            );
            sourceRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save research source", e);
        }
    }

    private ResearchSource deserializeSource(ResearchSourceEntity entity) {
        try {
            return mapper.readValue(entity.getFullSourceJson(), ResearchSource.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize research source", e);
        }
    }

    private RegisteredSourceContent toRegisteredSourceContent(ResearchSourceEntity entity, ResearchSource source) {
        if (entity.getRawContent() == null) {
            return null;
        }
        try {
            ExtractedWebContent extracted = entity.getExtractedContentJson() != null
                    ? mapper.readValue(entity.getExtractedContentJson(), ExtractedWebContent.class)
                    : null;
            return new RegisteredSourceContent(source, entity.getRawContent(), extracted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize extracted web content", e);
        }
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
