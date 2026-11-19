package org.wrj.haifa.ai.deerflow.research;

public record FetchRegistrationResult(
        RegisteredSourceContent stored,
        boolean cached,
        boolean deduplicatedByUrl,
        boolean deduplicatedByContentHash
) {
}
