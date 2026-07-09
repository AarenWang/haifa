package org.wrj.haifa.ai.deerflow.evidence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("EvidenceItemStoreV2")
public class EvidenceItemStore {

    private final EvidenceItemRepository repository;

    public EvidenceItemStore(EvidenceItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EvidenceItem create(String sourceId, String runId, String threadId, String workItemId, String summary, String claimSupportText, String locator, Double confidence) {
        EvidenceItem item = new EvidenceItem();
        item.setEvidenceId(UUID.randomUUID().toString());
        item.setSourceId(sourceId);
        item.setRunId(runId);
        item.setThreadId(threadId);
        item.setWorkItemId(workItemId);
        item.setSummary(summary);
        item.setClaimSupportText(claimSupportText);
        item.setLocator(locator);
        item.setConfidence(confidence != null ? confidence : 1.0);
        item.setAccepted(true);
        item.setCreatedAt(Instant.now());
        return repository.save(item);
    }

    @Transactional
    public EvidenceItem reject(String evidenceId, String reason) {
        return repository.findById(evidenceId).map(item -> {
            item.setAccepted(false);
            item.setRejectionReason(reason);
            return repository.save(item);
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EvidenceItem> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<EvidenceItem> findByThreadId(String threadId) {
        return repository.findByThreadId(threadId);
    }

    @Transactional(readOnly = true)
    public List<EvidenceItem> findBySourceId(String sourceId) {
        return repository.findBySourceId(sourceId);
    }
}
