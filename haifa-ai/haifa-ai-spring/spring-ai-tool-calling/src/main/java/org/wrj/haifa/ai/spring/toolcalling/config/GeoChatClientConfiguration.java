package org.wrj.haifa.ai.spring.toolcalling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.wrj.haifa.ai.spring.toolcalling.GeoChatClient;
import org.wrj.haifa.ai.spring.toolcalling.service.GeoKnowledgeService;

/**
 * Registers the {@link GeoChatClient} so it can be injected into controllers and
 * services just like an LLM backed {@link org.springframework.ai.chat.client.ChatClient}.
 */
@Configuration
public class GeoChatClientConfiguration {

    @Bean
    @Primary
    public GeoChatClient geoChatClient(GeoKnowledgeService geoKnowledgeService, ToolCallingProperties properties) {
        return new GeoChatClient(geoKnowledgeService, properties);
    }
}
