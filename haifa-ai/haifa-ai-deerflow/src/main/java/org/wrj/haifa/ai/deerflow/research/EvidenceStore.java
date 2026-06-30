package org.wrj.haifa.ai.deerflow.research;

import java.util.List;
import java.util.Optional;

public interface EvidenceStore {

    EvidenceItem save(EvidenceItem evidenceItem);

    List<EvidenceItem> listByThread(String threadId);

    List<EvidenceItem> listByRun(String runId);

    List<EvidenceItem> listBySourceId(String sourceId);

    List<EvidenceItem> listByDimension(String dimension);

    Optional<EvidenceItem> findById(String evidenceId);
}
