package org.wrj.haifa.ai.deerflow.model;

import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
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
            return Mono.just(fallbackAnswer());
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
                        .filter(SpringAiAgentModelClient::isRetryableFailure)
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

    @Override
    public Flux<ModelResponse> streamGenerate(ModelPrompt prompt) {
        ChatClient.Builder builder = this.chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("Spring AI ChatClient.Builder is not available. Returning fallback answer.");
            return Flux.just(fallbackAnswer());
        }
        long startTime = System.currentTimeMillis();
        long timeoutMs = Math.max(1_000, this.properties.getModelTimeout());
        log.info("Spring AI model streaming starting. model={}, timeoutMs={}, systemPromptChars={}, userPromptChars={}, messageCount={}",
                safe(prompt.modelName()), timeoutMs, length(prompt.systemPrompt()), length(prompt.effectiveUserPrompt()),
                prompt.messages().size());

        return Flux.defer(() -> {
            ChatClient.ChatClientRequestSpec request = builder.build()
                    .prompt()
                    .messages(toSpringAiMessages(prompt));

            ChatOptions chatOptions = chatOptionsFor(prompt);
            if (chatOptions != null) {
                request = request.options(chatOptions);
            }

            return request.stream().chatResponse()
                    .map(chatResponse -> {
                        if (chatResponse == null) {
                            return new ModelResponse("");
                        }
                        String content = "";
                        List<ModelToolCall> modelToolCalls = new ArrayList<>();

                        if (chatResponse.getResult() != null) {
                            org.springframework.ai.chat.model.Generation generation = chatResponse.getResult();
                            AssistantMessage assistantMessage = generation.getOutput();
                            if (assistantMessage != null) {
                                content = assistantMessage.getText();
                                if (content == null) {
                                    content = "";
                                }
                                if (assistantMessage.getToolCalls() != null) {
                                    for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
                                        modelToolCalls.add(new ModelToolCall(
                                                tc.id(),
                                                tc.name(),
                                                tc.arguments(),
                                                tc.type()
                                        ));
                                    }
                                }
                            }
                        }

                        String finishReason = null;
                        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
                            finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                        }

                        return new ModelResponse(content, modelToolCalls, List.of(), finishReason, Map.of());
                    });
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofMillis(timeoutMs))
        .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3))
                .filter(SpringAiAgentModelClient::isRetryableFailure)
                .doBeforeRetry(retrySignal -> {
                    log.warn("Spring AI model streaming failed. Retrying (attempt {}/3)... Error: {}",
                            retrySignal.totalRetries() + 1,
                            retrySignal.failure() != null ? retrySignal.failure().getMessage() : "unknown");
                })
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
        .doOnComplete(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Spring AI model streaming completed. model={}, durationMs={}",
                    safe(prompt.modelName()), duration);
        })
        .doOnError(ex -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Spring AI chat streaming failed. model={}, systemPromptChars={}, userPromptChars={}, messageCount={}, durationMs={}",
                    safe(prompt.modelName()),
                    length(prompt.systemPrompt()),
                    length(prompt.effectiveUserPrompt()),
                    prompt.messages().size(),
                    duration,
                    ex);
        })
        .onErrorMap(TimeoutException.class, ex ->
                new IllegalStateException("Model API streaming call timed out after " + timeoutMs + "ms", ex));
    }

    private static ModelResponse callSpringAi(ChatClient.Builder builder, ModelPrompt prompt) {
        ChatClient.ChatClientRequestSpec request = builder.build()
                .prompt()
                .messages(toSpringAiMessages(prompt));

        ChatOptions chatOptions = chatOptionsFor(prompt);
        if (chatOptions != null) {
            request = request.options(chatOptions);
        }

        org.springframework.ai.chat.model.ChatResponse chatResponse = request.call().chatResponse();
        if (chatResponse == null) {
            return new ModelResponse("");
        }

        String content = "";
        List<ModelToolCall> modelToolCalls = new ArrayList<>();

        if (chatResponse.getResult() != null) {
            org.springframework.ai.chat.model.Generation generation = chatResponse.getResult();
            AssistantMessage assistantMessage = generation.getOutput();
            if (assistantMessage != null) {
                content = assistantMessage.getText();
                if (content == null) {
                    content = "";
                }
                if (assistantMessage.getToolCalls() != null) {
                    for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
                        modelToolCalls.add(new ModelToolCall(
                                tc.id(),
                                tc.name(),
                                tc.arguments(),
                                tc.type()
                        ));
                    }
                }
            }
        }

        String finishReason = null;
        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
            finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        }

        return new ModelResponse(content, modelToolCalls, List.of(), finishReason, Map.of());
    }

    static List<Message> toSpringAiMessages(ModelPrompt prompt) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(prompt.systemPrompt())) {
            messages.add(new SystemMessage(prompt.systemPrompt()));
        }
        if (prompt.hasMessages()) {
            for (ModelMessage message : prompt.messages()) {
                Message springMessage = toSpringAiMessage(message);
                if (springMessage != null) {
                    messages.add(springMessage);
                }
            }
        } else if (StringUtils.hasText(prompt.userPrompt())) {
            messages.add(new UserMessage(prompt.userPrompt()));
        }
        return messages;
    }

    private static Message toSpringAiMessage(ModelMessage message) {
        if (message == null) {
            return null;
        }
        return switch (message.role()) {
            case SYSTEM -> new SystemMessage(message.content());
            case USER -> new UserMessage(message.content());
            case ASSISTANT -> AssistantMessage.builder()
                    .content(message.content())
                    .properties(message.metadata())
                    .toolCalls(message.toolCalls().stream()
                            .map(toolCall -> new AssistantMessage.ToolCall(
                                    blankToDefault(toolCall.id(), ""),
                                    blankToDefault(toolCall.type(), "tool_call"),
                                    blankToDefault(toolCall.name(), ""),
                                    ModelToolCallSanitizer.sanitizeArguments(toolCall.arguments())))
                            .toList())
                    .build();
            case TOOL -> ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse(
                            blankToDefault(message.toolCallId(), ""),
                            blankToDefault(message.name(), ""),
                            message.content())))
                    .metadata(message.metadata())
                    .build();
        };
    }

    static boolean isRetryableFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof WebClientResponseException responseException) {
                int status = responseException.getStatusCode().value();
                return status == 408 || status == 429 || status >= 500;
            }
            current = current.getCause();
        }
        return true;
    }

    static ChatOptions chatOptionsFor(ModelPrompt prompt) {
        if (prompt.toolDefinitions().isEmpty()) {
            return StringUtils.hasText(prompt.modelName())
                    ? ChatOptions.builder().model(prompt.modelName()).build()
                    : null;
        }
        ToolCallingChatOptions.Builder optionsBuilder = ToolCallingChatOptions.builder()
                .toolCallbacks(toToolCallbacks(prompt.toolDefinitions()))
                .internalToolExecutionEnabled(false);
        if (StringUtils.hasText(prompt.modelName())) {
            optionsBuilder.model(prompt.modelName());
        }
        return optionsBuilder.build();
    }

    static List<ToolCallback> toToolCallbacks(List<ModelToolDefinition> toolDefinitions) {
        return toolDefinitions.stream()
                .filter(tool -> StringUtils.hasText(tool.name()))
                .map(DeclarativeToolCallback::new)
                .map(ToolCallback.class::cast)
                .toList();
    }

    private record DeclarativeToolCallback(ModelToolDefinition modelToolDefinition) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return DefaultToolDefinition.builder()
                    .name(modelToolDefinition.name())
                    .description(modelToolDefinition.description())
                    .inputSchema(modelToolDefinition.inputSchema())
                    .build();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return ToolMetadata.builder().returnDirect(false).build();
        }

        @Override
        public String call(String toolInput) {
            return "Tool execution is managed by DeerFlow AgentLoop and is disabled inside SpringAiAgentModelClient.";
        }
    }

    private static ModelResponse fallbackAnswer() {
        String content = """
                Spring AI provider is not configured, so DeerFlow cannot generate a model answer right now.

                Please configure a Spring AI chat provider and retry this request.
                """;
        return new ModelResponse(content);
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value : "<backend-default>";
    }

    private static String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
