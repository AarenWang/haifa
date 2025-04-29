package me.haifa.block.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigInteger;


@Entity
@Table(name = "log_ext")
@Data
public class LogExtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id")
    private Long logId; // 外键关联原始日志表

    @Column(name = "event_type")
    private String eventType; // Transfer / Approval / Mint / Burn 等

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    @Column(name = "amount", columnDefinition = "NUMERIC")
    private BigInteger amount;

    @Column(name = "token_address")
    private String tokenAddress;

    // Getters and Setters...
}

