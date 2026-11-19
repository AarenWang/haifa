package org.wrj.haifa.ai.deerflow.voice.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "haifa.ai.deerflow.voice", name = "enabled", havingValue = "true")
public class VoiceWebSocketConfig {

    @Bean
    public HandlerMapping voiceWebSocketHandlerMapping(VoiceWebSocketHandler voiceWebSocketHandler,
                                                        VoiceProperties properties) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put(properties.getWebsocket().getPath(), voiceWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
