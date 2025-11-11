package org.wrj.haifa.ai.spring.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Demonstrates how to call the translation service once the Spring context is ready.
 */
@Component
public class LargeModelTranslationDemo {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final LargeModelTranslationService translator;
    private final LargeModelTranslationProperties properties;

    public LargeModelTranslationDemo(LargeModelTranslationService translator,
                                     LargeModelTranslationProperties properties) {
        this.translator = translator;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!StringUtils.hasText(this.properties.getToken())) {
            log.info("Skipping translation sample because no API token is configured.");
            return;
        }

        try {
            String sample = "Spring AI helps teams integrate large models via idiomatic abstractions.";
            String translation = this.translator.translate(sample, this.properties.getDefaultTargetLanguage());
            log.info("Sample translation result:\n{}", translation);
        }
        catch (Exception ex) {
            log.warn("Translation sample failed", ex);
        }
    }
}
