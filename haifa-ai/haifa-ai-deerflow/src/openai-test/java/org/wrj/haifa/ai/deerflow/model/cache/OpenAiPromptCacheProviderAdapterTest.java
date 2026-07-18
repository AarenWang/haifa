package org.wrj.haifa.ai.deerflow.model.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.prompt.PromptCachePlanner;

class OpenAiPromptCacheProviderAdapterTest {

    @Test
    void appliesStablePromptCacheKeyAndStreamingUsage() {
        DeerFlowProperties properties = new DeerFlowProperties();
        ModelPrompt prompt = PromptCachePlanner.enrich(
                new ModelPrompt("stable ".repeat(800), "dynamic", "gpt-4.1"), properties);

        OpenAiChatOptions options = (OpenAiChatOptions) new OpenAiPromptCacheProviderAdapter(properties)
                .buildOptions(prompt, java.util.List.of());

        assertThat(options.getPromptCacheKey()).isEqualTo(prompt.cacheContext().routingKey());
        assertThat(options.getStreamUsage()).isTrue();
    }

    @Test
    void honorsPromptCacheKeyKillSwitch() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getPromptCache().getOpenai().setPromptCacheKeyEnabled(false);
        ModelPrompt prompt = PromptCachePlanner.enrich(
                new ModelPrompt("stable ".repeat(800), "dynamic", "gpt-4.1"), properties);

        OpenAiChatOptions options = (OpenAiChatOptions) new OpenAiPromptCacheProviderAdapter(properties)
                .buildOptions(prompt, java.util.List.of());

        assertThat(options.getPromptCacheKey()).isNull();
    }
}
