package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchSourceMappingEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchSourceMappingRepository extends JpaRepository<ResearchSourceMappingEntity, String> {
    List<ResearchSourceMappingEntity> findByThreadId(String threadId);
    List<ResearchSourceMappingEntity> findByRunId(String runId);
    Optional<ResearchSourceMappingEntity> findBySourceIdAndThreadIdAndRunId(String sourceId, String threadId, String runId);
    void deleteBySourceIdAndThreadIdAndRunId(String sourceId, String threadId, String runId);
}
