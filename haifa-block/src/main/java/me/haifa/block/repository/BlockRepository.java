package me.haifa.block.repository;

import me.haifa.block.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface BlockRepository extends JpaRepository<BlockEntity, BigInteger> {
    // 可以添加自定义方法，如按 blockHash 查询等
}
