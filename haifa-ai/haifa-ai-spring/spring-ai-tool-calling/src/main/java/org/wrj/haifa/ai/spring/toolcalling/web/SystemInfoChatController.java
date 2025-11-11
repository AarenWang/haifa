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
import org.wrj.haifa.ai.spring.toolcalling.system.SystemInfoService;

/**
 * REST endpoint that exposes the local system inspection tool through the chat
 * client abstraction.
 */
@RestController
@RequestMapping("/api/system/chat")
public class SystemInfoChatController {

    private final ChatClient chatClient;
    private final SystemInfoService systemInfoService;
    private final ToolCallingProperties properties;

    public SystemInfoChatController(ChatClient chatClient, SystemInfoService systemInfoService,
            ToolCallingProperties properties) {
        this.chatClient = chatClient;
        this.systemInfoService = systemInfoService;
        this.properties = properties;
    }

    @PostMapping
    public ChatResponsePayload chat(@RequestBody ChatRequest request) {
        ChatClient.ChatClientRequestSpec spec = this.chatClient
                .prompt()
                .system("You are a diagnostic assistant that summarizes host level metrics.")
                .tools(this.systemInfoService)
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
