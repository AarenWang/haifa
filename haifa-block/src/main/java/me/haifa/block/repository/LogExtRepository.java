package me.haifa.block.repository;

import me.haifa.block.entity.LogEntity;
import me.haifa.block.entity.LogExtEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogExtRepository extends JpaRepository<LogExtEntity, Long> {
    // 例如查某 tx 的日志：
    // List<LogEntity> findByTxHash(String txHash);
}
