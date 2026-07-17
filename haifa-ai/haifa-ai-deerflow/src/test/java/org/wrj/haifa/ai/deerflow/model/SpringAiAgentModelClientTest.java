package org.wrj.haifa.ai.deerflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.wrj.haifa.ai.deerflow.DeerFlowApplication;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import reactor.test.StepVerifier;

class SpringAiAgentModelClientTest {

    @Test
    void callsModelUsingOpenAiEnvironment() {
        String apiKey = requiredEnvironmentVariable("OPENAI_API_KEY");
        String baseUrl = requiredEnvironmentVariable("OPENAI_BASE_URL");
        String model = requiredEnvironmentVariable("HAIFA_DEERFLOW_MODEL");

        Map<String, Object> properties = Map.of(
                "spring.ai.openai.api-key", apiKey,
                "spring.ai.openai.base-url", baseUrl,
                "haifa.ai.deerflow.model", model,
                "spring.main.lazy-initialization", true);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DeerFlowApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run()) {
            SpringAiAgentModelClient client = context.getBean(SpringAiAgentModelClient.class);
            ModelResponse response = client.generate(new ModelPrompt(
                            "You are a concise and helpful assistant.",
                            "Reply with a short greeting to confirm that the model connection works.",
                            model))
                    .block();

            assertThat(response).isNotNull();
            assertThat(response.content()).isNotBlank();
            System.out.println("SpringAiAgentModelClient response:");
            System.out.println(response.content());
        }
    }

    @Test
    void buildsStructuredToolCallingOptionsWithoutInternalExecution() {
        ModelToolDefinition definition = new ModelToolDefinition(
                "read_uploaded_file",
                "Read an uploaded file.",
                """
                        {
                          "type": "object",
                          "properties": {
                            "file_id": { "type": "string" }
                          }
                        }
                        """);

        ChatOptions options = SpringAiAgentModelClient.chatOptionsFor(
                new ModelPrompt("system", "user", "qwen-max", List.of(), List.of(definition)));

        assertThat(options).isInstanceOf(ToolCallingChatOptions.class);
        ToolCallingChatOptions toolOptions = (ToolCallingChatOptions) options;
        assertThat(toolOptions.getModel()).isEqualTo("qwen-max");
        assertThat(toolOptions.getInternalToolExecutionEnabled()).isFalse();
        assertThat(toolOptions.getToolCallbacks()).hasSize(1);

        ToolCallback callback = toolOptions.getToolCallbacks().getFirst();
        assertThat(callback.getToolDefinition().name()).isEqualTo("read_uploaded_file");
        assertThat(callback.getToolDefinition().description()).contains("Read an uploaded file");
        assertThat(callback.getToolDefinition().inputSchema()).contains("file_id");
    }

    @Test
    void fallbackAnswerDoesNotExposePromptOrConversationHistory() {
        DeerFlowProperties properties = new DeerFlowProperties();

        @SuppressWarnings("unchecked")
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SpringAiAgentModelClient client = new SpringAiAgentModelClient(provider, properties);
        String internalPrompt = """
                <conversationhistory>
                USER: Windows 有没有电量使用 电量消耗 情况的命令行API
                ASSISTANT:
                TOOL: Search results for Windows command line API to check battery usage
                </conversationhistory>
                """;

        StepVerifier.create(client.generate(new ModelPrompt("system", internalPrompt, "model")))
                .assertNext(response -> {
                    assertThat(response.content()).contains("Spring AI provider is not configured");
                    assertThat(response.content()).doesNotContain(
                            "<conversationhistory>",
                            "TOOL:",
                            "Prompt sent to the model adapter",
                            "Windows 有没有电量使用");
                })
                .verifyComplete();
    }

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

    @Test
    void retriesOnlyRecoverableProviderResponseErrors() {
        assertThat(SpringAiAgentModelClient.isRetryableFailure(
                WebClientResponseException.create(400, "Bad Request", HttpHeaders.EMPTY, new byte[0], null)))
                .isFalse();
        assertThat(SpringAiAgentModelClient.isRetryableFailure(
                WebClientResponseException.create(429, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null)))
                .isTrue();
        assertThat(SpringAiAgentModelClient.isRetryableFailure(
                WebClientResponseException.create(503, "Unavailable", HttpHeaders.EMPTY, new byte[0], null)))
                .isTrue();
    }

    @Test
    void sanitizesMalformedPersistedToolArgumentsBeforeProviderRequest() {
        ModelMessage assistant = new ModelMessage(
                ModelMessage.Role.ASSISTANT,
                "",
                List.of(new ModelToolCall("call-1", "ask_clarification", "{\"question\":\"unfinished")),
                null,
                null,
                java.util.Map.of());

        AssistantMessage springMessage = (AssistantMessage) SpringAiAgentModelClient.toSpringAiMessages(
                new ModelPrompt("system", "", "model", List.of(assistant))).get(1);

        assertThat(springMessage.getToolCalls()).singleElement()
                .extracting(AssistantMessage.ToolCall::arguments)
                .isEqualTo("{}");
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        Assumptions.assumeTrue(value != null && !value.isBlank(),
                () -> "Skipping live model test because " + name + " is not set");
        return value;
    }
}
