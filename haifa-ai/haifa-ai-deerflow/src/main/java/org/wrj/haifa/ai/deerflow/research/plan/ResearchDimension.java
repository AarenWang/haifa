package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.List;

/**
 * A dimension within a research plan, representing one angle or sub-topic to explore.
 */
public record ResearchDimension(
        String id,
        String title,
        String description,
        ResearchTaskStatus status,
        List<String> searchQueries,
        int expectedSourceCount,
        int actualSourceCount,
        int actualEvidenceCount,
        List<String> evidenceIds
) {

    public ResearchDimension {
        status = status == null ? ResearchTaskStatus.PENDING : status;
        searchQueries = searchQueries == null ? List.of() : List.copyOf(searchQueries);
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }

    public ResearchDimension withStatus(ResearchTaskStatus newStatus) {
        return new ResearchDimension(id, title, description, newStatus, searchQueries,
                expectedSourceCount, actualSourceCount, actualEvidenceCount, evidenceIds);
    }

    public ResearchDimension withSourceCount(int count) {
        return new ResearchDimension(id, title, description, status, searchQueries,
                expectedSourceCount, count, actualEvidenceCount, evidenceIds);
    }

    public ResearchDimension withEvidenceCount(int count) {
        return new ResearchDimension(id, title, description, status, searchQueries,
                expectedSourceCount, actualSourceCount, count, evidenceIds);
    }

    public ResearchDimension withEvidenceIds(List<String> ids) {
        return new ResearchDimension(id, title, description, status, searchQueries,
                expectedSourceCount, actualSourceCount, actualEvidenceCount, ids);
    }
}
