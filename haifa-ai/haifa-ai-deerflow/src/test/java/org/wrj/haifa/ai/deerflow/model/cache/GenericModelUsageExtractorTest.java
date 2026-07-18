package org.wrj.haifa.ai.deerflow.model.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;

class GenericModelUsageExtractorTest {

    @Test
    void doesNotReportUnknownCacheMissAsFullyUncached() {
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        Usage usage = mock(Usage.class);
        when(metadata.getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(100);
        when(usage.getCompletionTokens()).thenReturn(20);
        when(usage.getTotalTokens()).thenReturn(120);

        ModelUsage extracted = new GenericModelUsageExtractor().extractUsage(metadata, "model");

        assertThat(extracted.inputTokens()).isEqualTo(100L);
        assertThat(extracted.cacheReadInputTokens()).isNull();
        assertThat(extracted.uncachedInputTokens()).isNull();
        assertThat(extracted.cacheHitRate()).isNull();
    }
}
