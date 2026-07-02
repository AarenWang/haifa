package org.wrj.haifa.ai.deerflow.model;

import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallParser;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Component
public class SpringAiAgentModelClient implements AgentModelClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAgentModelClient.class);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final DeerFlowProperties properties;
    private final BiFunction<ChatClient.Builder, ModelPrompt, ModelResponse> modelCaller;

    @Autowired
    public SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            DeerFlowProperties properties) {
        this(chatClientBuilderProvider, properties, SpringAiAgentModelClient::callSpringAi);
    }

    SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            DeerFlowProperties properties,
            BiFunction<ChatClient.Builder, ModelPrompt, ModelResponse> modelCaller) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.properties = properties;
        this.modelCaller = modelCaller;
    }

    @Override
    public Mono<ModelResponse> generate(ModelPrompt prompt) {
        ChatClient.Builder builder = this.chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("Spring AI ChatClient.Builder is not available. Returning fallback answer.");
            return Mono.just(fallbackAnswer(prompt));
        }
        long startTime = System.currentTimeMillis();
        long timeoutMs = Math.max(1_000, this.properties.getModelTimeout());
        log.info("Spring AI model call starting. model={}, timeoutMs={}, systemPromptChars={}, userPromptChars={}, messageCount={}",
                safe(prompt.modelName()), timeoutMs, length(prompt.systemPrompt()), length(prompt.effectiveUserPrompt()),
                prompt.messages().size());
        return Mono.fromCallable(() -> this.modelCaller.apply(builder, prompt))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3))
                        .doBeforeRetry(retrySignal -> {
                            log.warn("Spring AI model call failed. Retrying (attempt {}/3)... Error: {}",
                                    retrySignal.totalRetries() + 1,
                                    retrySignal.failure() != null ? retrySignal.failure().getMessage() : "unknown");
                        })
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Spring AI model call succeeded. model={}, durationMs={}, answerLength={}, toolCalls={}",
                            safe(prompt.modelName()), duration, response.content().length(), response.toolCalls().size());
                })
                .doOnError(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Spring AI chat call failed. model={}, systemPromptChars={}, userPromptChars={}, messageCount={}, durationMs={}",
                            safe(prompt.modelName()),
                            length(prompt.systemPrompt()),
                            length(prompt.effectiveUserPrompt()),
                            prompt.messages().size(),
                            duration,
                            ex);
                })
                .onErrorMap(TimeoutException.class, ex ->
                        new IllegalStateException("Model API call timed out after " + timeoutMs + "ms", ex));
    }

    private static ModelResponse callSpringAi(ChatClient.Builder builder, ModelPrompt prompt) {
        String effectiveUserPrompt = prompt.effectiveUserPrompt();
        ChatClient.ChatClientRequestSpec request = builder.build()
                .prompt()
                .system(prompt.systemPrompt())
                .user(effectiveUserPrompt);

        if (StringUtils.hasText(prompt.modelName())) {
            request = request.options(ChatOptions.builder().model(prompt.modelName()).build());
        }

        org.springframework.ai.chat.model.ChatResponse chatResponse = request.call().chatResponse();
        if (chatResponse == null) {
            return new ModelResponse("");
        }

        String content = "";
        List<ModelToolCall> modelToolCalls = new ArrayList<>();

        if (chatResponse.getResult() != null) {
            org.springframework.ai.chat.model.Generation generation = chatResponse.getResult();
            content = generation.getOutput().getText();
            if (content == null) {
                content = "";
            }

            // Check if there are tool calls in AssistantMessage
            org.springframework.ai.chat.messages.AssistantMessage assistantMessage = generation.getOutput();
            if (assistantMessage != null && assistantMessage.getToolCalls() != null) {
                for (org.springframework.ai.chat.messages.AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
                    modelToolCalls.add(new ModelToolCall(
                        tc.id(),
                        tc.name(),
                        tc.arguments(),
                        tc.type()
                    ));
                }
            }
        }

        // If Spring AI model returned no structural tool calls, fallback to parsing content via ToolCallParser
        if (modelToolCalls.isEmpty()) {
            ToolCallParser parser = new ToolCallParser();
            List<ToolCallParser.ParsedToolCall> parsed = parser.parse(content);
            if (!parsed.isEmpty()) {
                for (ToolCallParser.ParsedToolCall ptc : parsed) {
                    modelToolCalls.add(new ModelToolCall(
                        UUID.randomUUID().toString(),
                        ptc.toolName(),
                        ptc.arguments()
                    ));
                }
                // Strip the XML/DSML tags from final response text
                content = parser.cleanResponseText(content);
            }
        } else {
            // Also clean any leftover xml/dsml tags from text if structural tool calls were returned
            ToolCallParser parser = new ToolCallParser();
            content = parser.cleanResponseText(content);
        }

        String finishReason = null;
        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
            finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        }

        return new ModelResponse(content, modelToolCalls, List.of(), finishReason, Map.of());
    }

    private static ModelResponse fallbackAnswer(ModelPrompt prompt) {
        String content = """
                Spring AI provider is not configured, so this prototype returned a deterministic fallback.

                The agent still completed the DeerFlow runtime path: run creation, safe local tools,
                prompt assembly, model adapter invocation, and SSE event emission.

                Prompt sent to the model adapter:
                %s
                """.formatted(prompt.effectiveUserPrompt());
        return new ModelResponse(content);
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value : "<backend-default>";
    }
}
