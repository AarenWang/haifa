package org.wrj.haifa.ai.deerflow.research;

import java.util.List;

public record FetchProcessingResult(
        FetchRegistrationResult registration,
        List<EvidenceItem> evidenceItems,
        String observation
) {

    public FetchProcessingResult {
        evidenceItems = evidenceItems == null ? List.of() : List.copyOf(evidenceItems);
        observation = observation == null ? "" : observation;
    }
}
