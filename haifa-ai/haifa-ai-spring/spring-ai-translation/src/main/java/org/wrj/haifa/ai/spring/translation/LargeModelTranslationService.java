package org.wrj.haifa.ai.spring.translation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Samples a translation call that constructs a translation prompt against a configured large model.
 */
@Service
public class LargeModelTranslationService {

    private static final String SYSTEM_PROMPT = "You are a succinct translation assistant, " +
            "and you only return translated text without explanation.";

    private final ChatClient chatClient;
    private final LargeModelTranslationProperties properties;

    public LargeModelTranslationService(ChatClient chatClient, LargeModelTranslationProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    /**
     * Translate the given text into {@code targetLanguage} or into the configured default if it is blank.
     */
    public String translate(String text, String targetLanguage) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String normalizedTarget = StringUtils.hasText(targetLanguage)
                ? targetLanguage
                : properties.getDefaultTargetLanguage();

        ChatClient.ChatClientRequestSpec request = this.chatClient.prompt()
                .system(SYSTEM_PROMPT);

        if (StringUtils.hasText(this.properties.getModel())) {
            ChatOptions options = ChatOptions.builder()
                    .model(this.properties.getModel())
                    .build();
            request = request.options(options);
        }

        String userInstruction = buildUserInstruction(text, normalizedTarget);
        return request
                .user(userInstruction)
                .call()
                .content();
    }

    private static String buildUserInstruction(String text, String targetLanguage) {
        return "Translate the following text into " + targetLanguage + ":\n" + text.trim();
    }
}
