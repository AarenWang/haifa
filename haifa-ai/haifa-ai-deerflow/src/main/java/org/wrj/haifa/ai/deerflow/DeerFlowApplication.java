package org.wrj.haifa.ai.deerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@SpringBootApplication
@EnableConfigurationProperties(DeerFlowProperties.class)
@EnableJpaRepositories(basePackages = "org.wrj.haifa.ai.deerflow.persistence.repository")
public class DeerFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeerFlowApplication.class, args);
    }
}
