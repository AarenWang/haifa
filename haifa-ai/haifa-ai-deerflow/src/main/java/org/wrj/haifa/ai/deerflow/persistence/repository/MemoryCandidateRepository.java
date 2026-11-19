package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.MemoryCandidateEntity;

@Repository
public interface MemoryCandidateRepository extends JpaRepository<MemoryCandidateEntity, String> {

    List<MemoryCandidateEntity> findByUserId(String userId);

    List<MemoryCandidateEntity> findByUserIdAndStatus(String userId, String status);
}
