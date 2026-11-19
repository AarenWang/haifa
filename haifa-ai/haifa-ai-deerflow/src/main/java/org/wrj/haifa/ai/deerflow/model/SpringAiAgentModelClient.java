package org.wrj.haifa.ai.deerflow.model;

import com.fasterxml.jackson.core.io.JsonEOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.ModelOptionsUtils;
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
import org.wrj.haifa.ai.deerflow.model.cache.GenericPromptCacheProviderAdapter;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheProviderAdapter;
import org.wrj.haifa.ai.deerflow.prompt.PromptCachePlanner;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Component
public class SpringAiAgentModelClient implements AgentModelClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAgentModelClient.class);

    private static final List<SpringAiProtocolStateCodec> codecs = List.of(new GoogleThoughtSignatureCodec());

    private static ModelProtocolState captureProtocolState(AssistantMessage message) {
        if (message == null) {
            return ModelProtocolState.empty();
        }
        for (SpringAiProtocolStateCodec codec : codecs) {
            ModelProtocolState state = codec.capture(message);
            if (state != null && !state.isEmpty()) {
                return state;
            }
        }
        return ModelProtocolState.empty();
    }

    private static Map<String, Object> restoreProtocolState(ModelProtocolState state) {
        if (state == null || state.isEmpty()) {
            return Map.of();
        }
        for (SpringAiProtocolStateCodec codec : codecs) {
            if (codec.supports(state.adapter())) {
                return codec.restore(state);
            }
        }
        throw new ModelProtocolStateException(
                "No Spring AI protocol state codec registered for adapter: " + state.adapter());
    }

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final DeerFlowProperties properties;
    private final BiFunction<ChatClient.Builder, ModelPrompt, ModelResponse> modelCaller;
    private final List<PromptCacheProviderAdapter> promptCacheAdapters;

    @Autowired
    public SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            DeerFlowProperties properties,
            ObjectProvider<PromptCacheProviderAdapter> promptCacheAdapters) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.properties = properties;
        this.modelCaller = null;
        this.promptCacheAdapters = promptCacheAdapters.orderedStream().toList();
    }

    SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            DeerFlowProperties properties) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.properties = properties;
        this.modelCaller = null;
        this.promptCacheAdapters = List.of();
    }

    SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            DeerFlowProperties properties,
            BiFunction<ChatClient.Builder, ModelPrompt, ModelResponse> modelCaller) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.properties = properties;
        this.modelCaller = modelCaller;
        this.promptCacheAdapters = List.of();
    }

    SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            DeerFlowProperties properties,
            BiFunction<ChatClient.Builder, ModelPrompt, ModelResponse> modelCaller,
            List<PromptCacheProviderAdapter> promptCacheAdapters) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.properties = properties;
        this.modelCaller = modelCaller;
        this.promptCacheAdapters = promptCacheAdapters == null ? List.of() : List.copyOf(promptCacheAdapters);
    }

    @Override
    public Mono<ModelResponse> generate(ModelPrompt prompt) {
        ModelPrompt effectivePrompt = PromptCachePlanner.enrich(prompt, this.properties);
        ChatClient.Builder builder = this.chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("Spring AI ChatClient.Builder is not available. Returning fallback answer.");
            return Mono.just(fallbackAnswer());
        }
        long startTime = System.currentTimeMillis();
        long timeoutMs = Math.max(1_000, this.properties.getModelTimeout());
        log.info("Spring AI model call starting. model={}, timeoutMs={}, systemPromptChars={}, userPromptChars={}, messageCount={}",
                safe(effectivePrompt.modelName()), timeoutMs, length(effectivePrompt.systemPrompt()), length(effectivePrompt.effectiveUserPrompt()),
                effectivePrompt.messages().size());
        return Mono.fromCallable(() -> {
                    if (this.modelCaller != null) {
                        return this.modelCaller.apply(builder, effectivePrompt);
                    }
                    return this.callSpringAi(builder, effectivePrompt);
                })
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
                            safe(effectivePrompt.modelName()), duration, response.content().length(), response.toolCalls().size());
                })
                .doOnError(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Spring AI chat call failed. model={}, systemPromptChars={}, userPromptChars={}, messageCount={}, durationMs={}, providerDetail={}",
                            safe(effectivePrompt.modelName()),
                            length(effectivePrompt.systemPrompt()),
                            length(effectivePrompt.effectiveUserPrompt()),
                            effectivePrompt.messages().size(),
                            duration,
                            providerFailureDetail(ex),
                            ex);
                })
                .onErrorMap(TimeoutException.class, ex ->
                        new IllegalStateException("Model API call timed out after " + timeoutMs + "ms", ex));
    }

    @Override
    public Flux<ModelResponse> streamGenerate(ModelPrompt prompt) {
        ModelPrompt effectivePrompt = PromptCachePlanner.enrich(prompt, this.properties);
        ChatClient.Builder builder = this.chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            log.warn("Spring AI ChatClient.Builder is not available. Returning fallback answer.");
            return Flux.just(fallbackAnswer());
        }
        long startTime = System.currentTimeMillis();
        long timeoutMs = Math.max(1_000, this.properties.getModelTimeout());
        log.info("Spring AI model streaming starting. model={}, timeoutMs={}, systemPromptChars={}, userPromptChars={}, messageCount={}",
                safe(effectivePrompt.modelName()), timeoutMs, length(effectivePrompt.systemPrompt()), length(effectivePrompt.effectiveUserPrompt()),
                effectivePrompt.messages().size());

        PromptCacheProviderAdapter adapter = adapterFor(effectivePrompt);

        Flux<ModelResponse> providerStream = Flux.defer(() -> {
            ChatClient.ChatClientRequestSpec request = builder.build()
                    .prompt()
                    .messages(toSpringAiMessages(effectivePrompt));

            ChatOptions chatOptions = adapter.buildOptions(effectivePrompt,
                    toToolCallbacks(effectivePrompt.toolDefinitions()));
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
                        ModelProtocolState protocolState = ModelProtocolState.empty();

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
                                protocolState = captureProtocolState(assistantMessage);
                            }
                        }

                        String finishReason = null;
                        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
                            finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                        }
                        org.wrj.haifa.ai.deerflow.model.cache.ModelUsage usage = adapter
                                .extractUsage(chatResponse.getMetadata(), effectivePrompt.modelName());

                        return new ModelResponse(content, modelToolCalls, List.of(), finishReason, Map.of(), protocolState, usage);
                    });
        });

        return recoverTruncatedJsonStream(providerStream, () -> {
            log.warn("Spring AI model stream ended with truncated JSON before its first response. "
                    + "Falling back to a non-streaming model call. model={}", safe(effectivePrompt.modelName()));
            return generate(effectivePrompt);
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
                    safe(effectivePrompt.modelName()), duration);
        })
                .doOnError(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Spring AI chat streaming failed. model={}, systemPromptChars={}, userPromptChars={}, messageCount={}, durationMs={}, providerDetail={}",
                            safe(effectivePrompt.modelName()),
                            length(effectivePrompt.systemPrompt()),
                            length(effectivePrompt.effectiveUserPrompt()),
                            effectivePrompt.messages().size(),
                            duration,
                            providerFailureDetail(ex),
                            ex);
                })
        .onErrorMap(TimeoutException.class, ex ->
                new IllegalStateException("Model API streaming call timed out after " + timeoutMs + "ms", ex));
    }

    static Flux<ModelResponse> recoverTruncatedJsonStream(Flux<ModelResponse> stream,
            Supplier<Mono<ModelResponse>> fallback) {
        return Flux.defer(() -> {
            AtomicBoolean responseReceived = new AtomicBoolean();
            return stream
                    .doOnNext(ignored -> responseReceived.set(true))
                    .onErrorResume(failure -> !responseReceived.get() && isJsonEofFailure(failure),
                            failure -> fallback.get().flux());
        });
    }

    static boolean isJsonEofFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof JsonEOFException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ModelResponse callSpringAi(ChatClient.Builder builder, ModelPrompt prompt) {
        PromptCacheProviderAdapter adapter = adapterFor(prompt);
        ChatClient.ChatClientRequestSpec request = builder.build()
                .prompt()
                .messages(toSpringAiMessages(prompt));

        ChatOptions chatOptions = adapter.buildOptions(prompt, toToolCallbacks(prompt.toolDefinitions()));
        if (chatOptions != null) {
            request = request.options(chatOptions);
        }

        org.springframework.ai.chat.model.ChatResponse chatResponse = request.call().chatResponse();
        if (chatResponse == null) {
            return new ModelResponse("");
        }

        String content = "";
        List<ModelToolCall> modelToolCalls = new ArrayList<>();
        ModelProtocolState protocolState = ModelProtocolState.empty();

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
                protocolState = captureProtocolState(assistantMessage);
            }
        }

        String finishReason = null;
        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
            finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        }

        org.wrj.haifa.ai.deerflow.model.cache.ModelUsage usage = adapter
                .extractUsage(chatResponse.getMetadata(), prompt.modelName());

        return new ModelResponse(content, modelToolCalls, List.of(), finishReason, Map.of(), protocolState, usage);
    }

    static List<Message> toSpringAiMessages(ModelPrompt prompt) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(prompt.systemPrompt())) {
            messages.add(new SystemMessage(prompt.systemPrompt()));
        }

        if (prompt.hasMessages()) {
            for (ModelMessage message : prompt.messages()) {
                if (message == null) {
                    continue;
                }
                if (message.role() == ModelMessage.Role.SYSTEM) {
                    if (StringUtils.hasText(message.content())) {
                        String envelope = """
                                <runtime_instruction type="system_control" trusted="true">
                                %s
                                </runtime_instruction>
                                """.formatted(message.content()).trim();
                        messages.add(new UserMessage(envelope));
                    }
                    continue;
                }
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
            case SYSTEM -> null; // Consolidated by toSpringAiMessages.
            case USER -> new UserMessage(message.content());
            case ASSISTANT -> {
                Map<String, Object> properties = new HashMap<>();
                if (message.metadata() != null) {
                    properties.putAll(message.metadata());
                }
                properties.remove("protocolState");
                if (message.protocolState() != null && !message.protocolState().isEmpty()) {
                    properties.putAll(restoreProtocolState(message.protocolState()));
                }
                yield AssistantMessage.builder()
                        .content(message.content())
                        .properties(properties)
                        .toolCalls(message.toolCalls().stream()
                                .map(toolCall -> new AssistantMessage.ToolCall(
                                        blankToDefault(toolCall.id(), ""),
                                        blankToDefault(toolCall.type(), "tool_call"),
                                        blankToDefault(toolCall.name(), ""),
                                        ModelToolCallSanitizer.sanitizeArguments(toolCall.arguments())))
                                .toList())
                        .build();
            }
            case TOOL -> ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse(
                            blankToDefault(message.toolCallId(), ""),
                            blankToDefault(message.name(), ""),
                            normalizeToolResponseData(message.content()))))
                    .metadata(message.metadata())
                    .build();
        };
    }

    /**
     * Spring AI's Google GenAI adapter deserializes every tool response as JSON before
     * building Gemini's {@code functionResponse.response} map. DeerFlow tools are also
     * allowed to return plain text, so expose non-object results through a stable JSON
     * envelope that remains valid for OpenAI-compatible providers as well.
     */
    private static String normalizeToolResponseData(String responseData) {
        String content = responseData == null ? "" : responseData;
        try {
            Object parsed = ModelOptionsUtils.OBJECT_MAPPER.readValue(content, Object.class);
            if (parsed instanceof Map<?, ?>) {
                return content;
            }
            return ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(Map.of("result", parsed));
        } catch (Exception ignored) {
            try {
                return ModelOptionsUtils.OBJECT_MAPPER.writeValueAsString(Map.of("result", content));
            } catch (Exception serializationFailure) {
                throw new IllegalStateException("Failed to serialize tool response", serializationFailure);
            }
        }
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

    private static String providerFailureDetail(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof WebClientResponseException responseException) {
                String responseBody = responseException.getResponseBodyAsString();
                return "httpStatus=" + responseException.getStatusCode().value()
                        + ", responseBody=" + safeProviderText(responseBody);
            }
            current = current.getCause();
        }
        return "exception=" + safeProviderText(failure == null ? "" : failure.getMessage());
    }

    private static String safeProviderText(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        String sanitized = value.replaceAll("(?i)(authorization|api[-_ ]?key)\\s*[:=]\\s*[^,\\s]+", "$1=[REDACTED]")
                .replaceAll("\\s+", " ").trim();
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500) + "...";
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

    private PromptCacheProviderAdapter adapterFor(ModelPrompt prompt) {
        String requestedProvider = prompt.cacheContext() == null ? "" : prompt.cacheContext().provider();
        if (StringUtils.hasText(requestedProvider)) {
            for (PromptCacheProviderAdapter adapter : this.promptCacheAdapters) {
                if (requestedProvider.equalsIgnoreCase(adapter.providerId())) {
                    return adapter;
                }
            }
        }
        for (PromptCacheProviderAdapter adapter : this.promptCacheAdapters) {
            if (adapter.supports(prompt)) {
                return adapter;
            }
        }
        if (this.promptCacheAdapters.size() == 1) {
            return this.promptCacheAdapters.get(0);
        }
        return new GenericPromptCacheProviderAdapter();
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
