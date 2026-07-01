package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.MemoryFactEntity;

@Repository
public interface MemoryFactRepository extends JpaRepository<MemoryFactEntity, String> {

    List<MemoryFactEntity> findByUserId(String userId);

    List<MemoryFactEntity> findByUserIdAndStatus(String userId, String status);
}
