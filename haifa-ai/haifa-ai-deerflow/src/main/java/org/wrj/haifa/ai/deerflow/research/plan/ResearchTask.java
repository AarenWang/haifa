package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.List;

/**
 * A research task derived from a plan dimension, trackable within the research loop.
 */
public record ResearchTask(
        String id,
        String threadId,
        String title,
        String dimension,
        ResearchTaskStatus status,
        List<String> evidenceIds,
        String runId
) {

    public ResearchTask {
        status = status == null ? ResearchTaskStatus.PENDING : status;
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }

    public ResearchTask withStatus(ResearchTaskStatus newStatus) {
        return new ResearchTask(id, threadId, title, dimension, newStatus, evidenceIds, runId);
    }

    public ResearchTask withEvidenceIds(List<String> ids) {
        return new ResearchTask(id, threadId, title, dimension, status, ids, runId);
    }
}
