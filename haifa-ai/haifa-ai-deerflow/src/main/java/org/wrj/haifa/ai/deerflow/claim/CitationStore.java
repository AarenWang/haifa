package org.wrj.haifa.ai.deerflow.claim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CitationStore {

    private final CitationRepository repository;

    public CitationStore(CitationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Citation create(String claimId, String evidenceId, String sourceId, String locator, String status) {
        Citation citation = new Citation();
        citation.setCitationId(UUID.randomUUID().toString());
        citation.setClaimId(claimId);
        citation.setEvidenceId(evidenceId);
        citation.setSourceId(sourceId);
        citation.setLocator(locator);
        citation.setStatus(status != null ? status : "valid");
        citation.setVerifiedAt(Instant.now());
        return repository.save(citation);
    }

    @Transactional(readOnly = true)
    public List<Citation> findByClaimId(String claimId) {
        return repository.findByClaimId(claimId);
    }

    @Transactional(readOnly = true)
    public List<Citation> findBySourceId(String sourceId) {
        return repository.findBySourceId(sourceId);
    }
}
