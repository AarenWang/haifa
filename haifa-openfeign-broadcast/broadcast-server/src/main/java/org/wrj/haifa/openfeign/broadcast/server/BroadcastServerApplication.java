package org.wrj.haifa.openfeign.broadcast.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "org.wrj.haifa.openfeign.broadcast")
@EnableDiscoveryClient
public class BroadcastServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BroadcastServerApplication.class, args);
    }
}
