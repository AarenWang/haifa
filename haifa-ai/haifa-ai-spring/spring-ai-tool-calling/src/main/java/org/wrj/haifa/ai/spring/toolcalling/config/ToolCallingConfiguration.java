package org.wrj.haifa.ai.spring.toolcalling.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Central configuration for the tool calling sample. It exposes the configuration
 * properties and the specialized {@link WebClient} used by {@code GeoKnowledgeService}.
 */
@Configuration
@EnableConfigurationProperties(ToolCallingProperties.class)
public class ToolCallingConfiguration {

    @Bean
    public WebClient geoKnowledgeWebClient(WebClient.Builder baseBuilder, ToolCallingProperties properties) {
        ToolCallingProperties.GeoProperties geo = properties.getGeo();
        WebClient.Builder builder = baseBuilder.clone()
                .baseUrl(geo.getBaseUrl())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(512 * 1024))
                        .build())
                .defaultHeader("User-Agent", "HaifaGeoKnowledgeClient/1.0")
                .defaultHeader("Accept", "application/json");

        if (StringUtils.hasText(geo.getApiKey())) {
            builder.defaultHeader("X-API-Key", geo.getApiKey());
        }

        return builder.build();
    }
}
