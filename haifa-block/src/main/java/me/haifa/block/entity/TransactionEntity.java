package me.haifa.block.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;

@Entity
@Table(name = "transactions")
@Data
public class TransactionEntity {

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "block_number")
    private BigInteger blockNumber;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    private BigInteger value;

    private BigInteger gas;

    @Column(name = "gas_price")
    private BigInteger gasPrice;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(name = "normalized_input", columnDefinition = "TEXT")
    private String normalizedInput;

    // Getters and Setters ...
}
