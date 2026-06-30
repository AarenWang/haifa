package org.wrj.haifa.ai.deerflow.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class SpringAiAgentModelClient implements AgentModelClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAgentModelClient.class);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public SpringAiAgentModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public Mono<String> generate(ModelPrompt prompt) {
        ChatClient.Builder builder = this.chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return Mono.just(fallbackAnswer(prompt));
        }
        return Mono.fromCallable(() -> callSpringAi(builder, prompt))
                .doOnError(ex -> log.error("Spring AI chat call failed. model={}, systemPromptChars={}, userPromptChars={}",
                        safe(prompt.modelName()),
                        length(prompt.systemPrompt()),
                        length(prompt.userPrompt()),
                        ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static String callSpringAi(ChatClient.Builder builder, ModelPrompt prompt) {
        ChatClient.ChatClientRequestSpec request = builder.build()
                .prompt()
                .system(prompt.systemPrompt())
                .user(prompt.userPrompt());

        if (StringUtils.hasText(prompt.modelName())) {
            request = request.options(ChatOptions.builder().model(prompt.modelName()).build());
        }
        return request.call().content();
    }

    private static String fallbackAnswer(ModelPrompt prompt) {
        return """
                Spring AI provider is not configured, so this prototype returned a deterministic fallback.

                The agent still completed the DeerFlow runtime path: run creation, safe local tools,
                prompt assembly, model adapter invocation, and SSE event emission.

                Prompt sent to the model adapter:
                %s
                """.formatted(prompt.userPrompt());
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value : "<backend-default>";
    }
}
