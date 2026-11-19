package org.wrj.haifa.ai.deerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@SpringBootApplication
@EnableConfigurationProperties(DeerFlowProperties.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "org.wrj.haifa.ai.deerflow.persistence.repository",
    "org.wrj.haifa.ai.deerflow.runstate",
    "org.wrj.haifa.ai.deerflow.work",
    "org.wrj.haifa.ai.deerflow.source",
    "org.wrj.haifa.ai.deerflow.evidence",
    "org.wrj.haifa.ai.deerflow.claim",
    "org.wrj.haifa.ai.deerflow.budget",
    "org.wrj.haifa.ai.deerflow.quality",
    "org.wrj.haifa.ai.deerflow.threadfile",
    "org.wrj.haifa.ai.deerflow.voice.persistence.repository"
})
public class DeerFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeerFlowApplication.class, args);
    }
}
