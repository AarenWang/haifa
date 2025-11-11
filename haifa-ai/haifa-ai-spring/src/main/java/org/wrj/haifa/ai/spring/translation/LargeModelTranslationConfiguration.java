package org.wrj.haifa.ai.spring.translation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LargeModelTranslationProperties.class)
public class LargeModelTranslationConfiguration {
}
