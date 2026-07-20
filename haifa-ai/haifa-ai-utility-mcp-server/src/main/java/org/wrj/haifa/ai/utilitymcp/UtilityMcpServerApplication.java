package org.wrj.haifa.ai.utilitymcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

@SpringBootApplication
@EnableConfigurationProperties(UtilityMcpProperties.class)
public class UtilityMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtilityMcpServerApplication.class, args);
    }
}
