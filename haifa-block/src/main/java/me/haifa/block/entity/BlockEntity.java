package me.haifa.block.entity;


import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;


@Entity
@Table(name = "blocks")
@Data
public class BlockEntity {

    @Id
    @Column(name = "block_number")
    private BigInteger blockNumber;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "parent_hash")
    private String parentHash;

    private BigInteger timestamp;

    @Column(name = "tx_count")
    private Integer txCount;

    // Getters and Setters ...
}