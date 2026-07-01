package org.wrj.haifa.ai.deerflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import reactor.test.StepVerifier;

class SpringAiAgentModelClientTest {

    @Test
    void appliesConfiguredModelTimeoutAndRetries() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setModelTimeout(50);

        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(ChatClient.Builder.class));

        SpringAiAgentModelClient client = new SpringAiAgentModelClient(provider, properties, (builder, prompt) -> {
            try {
                Thread.sleep(1_500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return new ModelResponse("late response");
        });

        // Timeout is 1000ms. Since it times out, it retries 3 times (each with 3s delay).
        // Total time will be ~13s. We set verify timeout to 18 seconds.
        StepVerifier.create(client.generate(new ModelPrompt("system", "user", "model")))
                .expectErrorSatisfies(ex -> assertThat(ex)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Model API call timed out after 1000ms"))
                .verify(Duration.ofSeconds(18));
    }

    @Test
    void retriesOnFailureUpToThreeTimes() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setModelTimeout(2000);

        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(ChatClient.Builder.class));

        AtomicInteger callCount = new AtomicInteger();
        SpringAiAgentModelClient client = new SpringAiAgentModelClient(provider, properties, (builder, prompt) -> {
            callCount.incrementAndGet();
            throw new RuntimeException("Model connection failed");
        });

        // 3 retries * 3 seconds delay = ~9 seconds total delay. We verify with 15s timeout.
        StepVerifier.create(client.generate(new ModelPrompt("system", "user", "model")))
                .expectErrorSatisfies(ex -> assertThat(ex.getMessage()).contains("Model connection failed"))
                .verify(Duration.ofSeconds(15));

        // 1 original attempt + 3 retries = 4 total attempts
        assertThat(callCount.get()).isEqualTo(4);
    }
}
