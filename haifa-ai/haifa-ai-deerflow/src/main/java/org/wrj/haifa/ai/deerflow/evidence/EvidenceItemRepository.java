package org.wrj.haifa.ai.deerflow.evidence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("EvidenceItemRepositoryV2")
public interface EvidenceItemRepository extends JpaRepository<EvidenceItem, String> {
    List<EvidenceItem> findByRunId(String runId);
    List<EvidenceItem> findByThreadId(String threadId);
    List<EvidenceItem> findBySourceId(String sourceId);
}
