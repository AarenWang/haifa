package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.CompressionCacheEntity;

@Repository
public interface CompressionCacheRepository extends JpaRepository<CompressionCacheEntity, String> {

    Optional<CompressionCacheEntity> findByCacheKeyAndExpiresAtAfter(String cacheKey, Instant now);

    List<CompressionCacheEntity> findAllByOrderByLastAccessedAtAsc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM CompressionCacheEntity e WHERE e.expiresAt <= :now")
    int deleteExpiredBefore(Instant now);
}
