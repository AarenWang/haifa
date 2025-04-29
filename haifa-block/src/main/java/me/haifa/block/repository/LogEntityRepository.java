package me.haifa.block.repository;

import me.haifa.block.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntityRepository extends JpaRepository<LogEntity, Long> {
    // 例如查某 tx 的日志：
    // List<LogEntity> findByTxHash(String txHash);
}
