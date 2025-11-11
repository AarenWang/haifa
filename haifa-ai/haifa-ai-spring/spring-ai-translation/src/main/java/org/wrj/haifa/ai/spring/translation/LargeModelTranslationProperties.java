package org.wrj.haifa.ai.spring.translation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the translation sample. The properties are intentionally simple so that
 * different tokens or models can be swapped without touching the code.
 */
@ConfigurationProperties(prefix = "haifa.ai.translation")
public class LargeModelTranslationProperties {

    /**
     * The model identifier to pass to the large model provider.
     */
    private String model = "gpt-4o-mini";

    /**
     * Authorization token that will be supplied through {@code spring.ai.openai.chat.api-key}.
     */
    private String token;

    /**
     * Language used when no explicit target language is provided.
     */
    private String defaultTargetLanguage = "English";

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDefaultTargetLanguage() {
        return defaultTargetLanguage;
    }

    public void setDefaultTargetLanguage(String defaultTargetLanguage) {
        this.defaultTargetLanguage = defaultTargetLanguage;
    }
}
