package me.haifa.block.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "logs")
@Data
public class LogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String txHash;
    private String address;

    private String topic0;
    private String topic1;
    private String topic2;
    private String topic3;

    @Column(columnDefinition = "TEXT")
    private String data;

    // 不添加解码字段
    // Getters and Setters ...
}
