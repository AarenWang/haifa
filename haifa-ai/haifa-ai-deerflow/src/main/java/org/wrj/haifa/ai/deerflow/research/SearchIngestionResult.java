package org.wrj.haifa.ai.deerflow.research;

import java.util.List;

public record SearchIngestionResult(
        List<SourceCandidateRegistration> registrations,
        String observation
) {

    public SearchIngestionResult {
        registrations = registrations == null ? List.of() : List.copyOf(registrations);
        observation = observation == null ? "" : observation;
    }
}
