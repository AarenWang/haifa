package org.wrj.haifa.ai.deerflow.claim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClaimStore {

    private final ClaimRepository repository;

    public ClaimStore(ClaimRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Claim create(String runId, String threadId, String artifactId, String text, List<String> evidenceIds, Double confidence, String status) {
        Claim claim = new Claim();
        claim.setClaimId(UUID.randomUUID().toString());
        claim.setRunId(runId);
        claim.setThreadId(threadId);
        claim.setArtifactId(artifactId);
        claim.setText(text);
        claim.setSupportEvidenceIds(evidenceIds);
        claim.setConfidence(confidence != null ? confidence : 1.0);
        claim.setStatus(status != null ? status : "draft");
        claim.setCreatedAt(Instant.now());
        claim.setUpdatedAt(Instant.now());
        return repository.save(claim);
    }

    @Transactional
    public Claim updateStatus(String claimId, String status) {
        return repository.findById(claimId).map(claim -> {
            claim.setStatus(status);
            claim.setUpdatedAt(Instant.now());
            return repository.save(claim);
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Claim> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<Claim> findByThreadId(String threadId) {
        return repository.findByThreadId(threadId);
    }
}
