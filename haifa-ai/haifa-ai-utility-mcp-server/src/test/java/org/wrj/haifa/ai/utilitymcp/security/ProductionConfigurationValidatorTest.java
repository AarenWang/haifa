package org.wrj.haifa.ai.utilitymcp.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

class ProductionConfigurationValidatorTest {

    @Test
    void productionFailsClosedWithoutIssuerAudienceAndOrigin() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        UtilityMcpProperties properties = new UtilityMcpProperties();
        properties.getSecurity().setAllowMissingOrigin(false);

        assertThatThrownBy(() -> new ProductionConfigurationValidator(environment, properties)
                .run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);

        environment.setProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://issuer.example");
        properties.getSecurity().setAllowedOrigins(List.of("https://agent.example"));
        assertThatThrownBy(() -> new ProductionConfigurationValidator(environment, properties)
                .run(new DefaultApplicationArguments())).hasMessageContaining("audience");
    }
}
