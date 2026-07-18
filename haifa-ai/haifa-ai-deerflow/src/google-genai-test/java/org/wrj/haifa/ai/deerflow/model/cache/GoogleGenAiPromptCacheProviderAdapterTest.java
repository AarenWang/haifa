package org.wrj.haifa.ai.deerflow.model.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.prompt.PromptCachePlanner;

class GoogleGenAiPromptCacheProviderAdapterTest {

    @Test
    void appliesConfiguredImplicitCacheOptionsWithoutOverridingIncludeThoughts() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getPromptCache().getGoogle().setAutoCacheEnabled(true);
        properties.getPromptCache().getGoogle().setAutoCacheThreshold(4096);
        properties.getPromptCache().getGoogle().setAutoCacheTtl("PT45M");
        ModelPrompt prompt = PromptCachePlanner.enrich(
                new ModelPrompt("stable ".repeat(800), "dynamic", "gemini-2.5-flash"), properties);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) new GoogleGenAiPromptCacheProviderAdapter(properties)
                .buildOptions(prompt, java.util.List.of());

        assertThat(options.getAutoCacheThreshold()).isEqualTo(4096);
        assertThat(options.getAutoCacheTtl()).isEqualTo(java.time.Duration.ofMinutes(45));
        assertThat(options.getIncludeExtendedUsageMetadata()).isTrue();
        assertThat(options.getIncludeThoughts()).isNull();
    }
}
