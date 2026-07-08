package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchSourceEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchSourceRepository extends JpaRepository<ResearchSourceEntity, String> {
    Optional<ResearchSourceEntity> findByNormalizedUrl(String normalizedUrl);
    Optional<ResearchSourceEntity> findByContentHash(String contentHash);
    List<ResearchSourceEntity> findByThreadId(String threadId);
    List<ResearchSourceEntity> findByRunId(String runId);
}
