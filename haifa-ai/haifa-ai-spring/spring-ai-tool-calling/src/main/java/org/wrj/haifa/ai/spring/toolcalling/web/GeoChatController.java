package org.wrj.haifa.ai.spring.toolcalling.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.ai.spring.toolcalling.config.ToolCallingProperties;
import org.wrj.haifa.ai.spring.toolcalling.model.ChatRequest;
import org.wrj.haifa.ai.spring.toolcalling.model.ChatResponsePayload;
import org.wrj.haifa.ai.spring.toolcalling.service.GeoKnowledgeService;

/**
 * REST endpoint that demonstrates how {@link ChatClient} driven tool calling can be
 * exposed over HTTP.
 */
@RestController
@RequestMapping("/api/geo/chat")
public class GeoChatController {

    private final ChatClient chatClient;
    private final GeoKnowledgeService geoKnowledgeService;
    private final ToolCallingProperties properties;

    public GeoChatController(ChatClient chatClient, GeoKnowledgeService geoKnowledgeService,
            ToolCallingProperties properties) {
        this.chatClient = chatClient;
        this.geoKnowledgeService = geoKnowledgeService;
        this.properties = properties;
    }

    @PostMapping
    public ChatResponsePayload chat(@RequestBody ChatRequest request) {
        ChatClient.ChatClientRequestSpec spec = this.chatClient
                .prompt()
                .system("You are a helpful travel assistant that explains geographic locations succinctly.")
                .tools(this.geoKnowledgeService)
                .user(request.message());

        if (StringUtils.hasText(request.model())) {
            spec = spec.options(ChatOptions.builder().model(request.model()).build());
        }
        else if (StringUtils.hasText(this.properties.getModel())) {
            spec = spec.options(ChatOptions.builder().model(this.properties.getModel()).build());
        }

        String answer = spec.call().content();
        return new ChatResponsePayload(answer);
    }
}
