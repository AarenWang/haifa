package org.wrj.haifa.ai.deerflow.research;

import java.time.Instant;
import java.util.List;

public record CitationRecord(
        String citationId,
        String threadId,
        String runId,
        String sourceId,
        List<String> evidenceIds,
        String claim,
        String marker,
        Instant citedAt
) {

    public CitationRecord {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        claim = claim == null ? "" : claim;
        marker = marker == null ? "" : marker;
    }
}
