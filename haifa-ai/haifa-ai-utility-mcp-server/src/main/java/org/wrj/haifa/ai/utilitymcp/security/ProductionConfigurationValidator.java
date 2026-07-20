package org.wrj.haifa.ai.utilitymcp.security;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

@Component
public class ProductionConfigurationValidator implements ApplicationRunner {

    private final Environment environment;
    private final UtilityMcpProperties properties;

    public ProductionConfigurationValidator(Environment environment, UtilityMcpProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("production");
        if (!production) return;
        String issuer = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("Production MCP server requires a JWT issuer-uri");
        }
        if (properties.getSecurity().getAllowedOrigins().isEmpty()) {
            throw new IllegalStateException("Production MCP server requires an explicit Origin allowlist");
        }
        if (properties.getSecurity().isAllowMissingOrigin()) {
            throw new IllegalStateException("Production MCP server must reject requests with a missing Origin");
        }
        if (!StringUtils.hasText(properties.getSecurity().getAudience())) {
            throw new IllegalStateException("Production MCP server requires an explicit JWT audience");
        }
        if (properties.getSecurity().getRequestsPerMinute() <= 0) {
            throw new IllegalStateException("Production MCP server requires a positive rate limit");
        }
    }
}
