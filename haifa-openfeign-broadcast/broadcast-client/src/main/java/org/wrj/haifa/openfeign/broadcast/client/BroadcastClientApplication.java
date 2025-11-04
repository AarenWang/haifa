package org.wrj.haifa.openfeign.broadcast.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "org.wrj.haifa.openfeign.broadcast")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "org.wrj.haifa.openfeign.broadcast.client.feign")
public class BroadcastClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(BroadcastClientApplication.class, args);
    }
}
