package org.wrj.haifa.ai.deerflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import reactor.test.StepVerifier;

class SpringAiAgentModelClientTest {

    @Test
    void appliesConfiguredModelTimeout() {
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

        StepVerifier.create(client.generate(new ModelPrompt("system", "user", "model")))
                .expectErrorSatisfies(ex -> assertThat(ex)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Model API call timed out after 1000ms"))
                .verify(Duration.ofSeconds(2));
    }
}
