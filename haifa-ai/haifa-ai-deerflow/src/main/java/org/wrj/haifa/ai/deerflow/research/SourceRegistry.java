package org.wrj.haifa.ai.deerflow.research;

import java.util.List;
import java.util.Optional;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

public interface SourceRegistry {

    String normalizeUrl(String url);

    SourceCandidateRegistration registerCandidate(String threadId, String runId, SearchResultCandidate candidate);

    FetchRegistrationResult registerFetched(
            String threadId,
            String runId,
            String url,
            String rawContent,
            ExtractedWebContent extractedContent,
            ResearchSourceType sourceType,
            double credibility
    );

    Optional<ResearchSource> findBySourceId(String sourceId);

    Optional<RegisteredSourceContent> findFetchedByUrl(String url);

    Optional<ResearchSource> findByUrl(String url);

    List<ResearchSource> listByThread(String threadId);

    List<ResearchSource> listByRun(String runId);
}
