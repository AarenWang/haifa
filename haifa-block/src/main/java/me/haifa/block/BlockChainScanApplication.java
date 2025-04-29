package me.haifa.block;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "me.haifa.block")
@EnableAsync
public class BlockChainScanApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockChainScanApplication.class, args);
    }
}
