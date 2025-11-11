package org.wrj.haifa.ai.spring.toolcalling;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.spring.toolcalling.config.ToolCallingProperties;
import org.wrj.haifa.ai.spring.toolcalling.model.GeoKnowledgeSummary;
import org.wrj.haifa.ai.spring.toolcalling.service.GeoKnowledgeService;
import org.wrj.haifa.ai.spring.toolcalling.tool.GeoTool;

/**
 * Minimal {@link ChatClient} implementation that routes incoming messages to the
 * {@link GeoKnowledgeService}. The goal is to provide an end-to-end example of tool
 * calling without depending on a remote LLM provider.
 */
public class GeoChatClient implements ChatClient {

    private final GeoKnowledgeService geoKnowledgeService;
    private final ToolCallingProperties properties;
    private final ChatOptions defaultOptions;

    public GeoChatClient(GeoKnowledgeService geoKnowledgeService, ToolCallingProperties properties) {
        this(geoKnowledgeService, properties, null);
    }

    private GeoChatClient(GeoKnowledgeService geoKnowledgeService, ToolCallingProperties properties,
            @Nullable ChatOptions defaultOptions) {
        this.geoKnowledgeService = geoKnowledgeService;
        this.properties = properties;
        this.defaultOptions = defaultOptions;
    }

    @Override
    public ChatClientRequestSpec prompt() {
        return new GeoRequestSpec();
    }

    @Override
    public ChatClientRequestSpec prompt(String prompt) {
        return prompt().user(prompt);
    }

    @Override
    public ChatClientRequestSpec prompt(Prompt prompt) {
        throw new UnsupportedOperationException("Prompt objects are not supported by GeoChatClient.");
    }

    @Override
    public Builder mutate() {
        return new GeoChatClientBuilder();
    }

    private class GeoRequestSpec implements ChatClientRequestSpec {

        private final List<Object> tools = new ArrayList<>();
        private ChatOptions options = defaultOptions;
        private String systemMessage;
        private String userMessage;

        @Override
        public Builder mutate() {
            return new GeoChatClientBuilder();
        }

        @Override
        public ChatClientRequestSpec advisors(Consumer<AdvisorSpec> advisors) {
            return this;
        }

        @Override
        public ChatClientRequestSpec advisors(Advisor... advisors) {
            return this;
        }

        @Override
        public ChatClientRequestSpec advisors(List<Advisor> advisors) {
            return this;
        }

        @Override
        public ChatClientRequestSpec messages(Message... messages) {
            return this;
        }

        @Override
        public ChatClientRequestSpec messages(List<Message> messages) {
            return this;
        }

        @Override
        public <T extends ChatOptions> ChatClientRequestSpec options(T options) {
            this.options = options;
            return this;
        }

        @Override
        public ChatClientRequestSpec toolNames(String... toolNames) {
            return this;
        }

        @Override
        public ChatClientRequestSpec tools(Object... tools) {
            this.tools.addAll(Arrays.asList(tools));
            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(org.springframework.ai.tool.ToolCallback... toolCallbacks) {
            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(List<org.springframework.ai.tool.ToolCallback> toolCallbacks) {
            return this;
        }

        @Override
        public ChatClientRequestSpec toolCallbacks(org.springframework.ai.tool.ToolCallbackProvider... providers) {
            return this;
        }

        @Override
        public ChatClientRequestSpec toolContext(Map<String, Object> toolContext) {
            return this;
        }

        @Override
        public ChatClientRequestSpec system(String content) {
            this.systemMessage = content;
            return this;
        }

        @Override
        public ChatClientRequestSpec system(Resource resource, Charset charset) {
            throw new UnsupportedOperationException("System resources are not supported by GeoChatClient.");
        }

        @Override
        public ChatClientRequestSpec system(Resource resource) {
            throw new UnsupportedOperationException("System resources are not supported by GeoChatClient.");
        }

        @Override
        public ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer) {
            return this;
        }

        @Override
        public ChatClientRequestSpec user(String content) {
            this.userMessage = content;
            return this;
        }

        @Override
        public ChatClientRequestSpec user(Resource resource, Charset charset) {
            throw new UnsupportedOperationException("User resources are not supported by GeoChatClient.");
        }

        @Override
        public ChatClientRequestSpec user(Resource resource) {
            throw new UnsupportedOperationException("User resources are not supported by GeoChatClient.");
        }

        @Override
        public ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
            return this;
        }

        @Override
        public ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer) {
            return this;
        }

        @Override
        public CallResponseSpec call() {
            String query = resolveQuery();
            GeoKnowledgeSummary summary = locateToolResult(query)
                    .orElseGet(() -> geoKnowledgeService.lookup(query));
            return new GeoCallResponseSpec(buildContent(summary));
        }

        @Override
        public StreamResponseSpec stream() {
            throw new UnsupportedOperationException("Streaming is not supported by GeoChatClient.");
        }

        private String resolveQuery() {
            if (StringUtils.hasText(userMessage)) {
                return userMessage.trim();
            }
            if (StringUtils.hasText(systemMessage)) {
                return systemMessage.trim();
            }
            return "";
        }

        private Optional<GeoKnowledgeSummary> locateToolResult(String query) {
            return tools.stream()
                    .map(tool -> invokeTool(tool, query))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        }

        private Optional<GeoKnowledgeSummary> invokeTool(Object tool, String query) {
            Method matched = findToolMethod(tool.getClass());
            if (matched == null) {
                return Optional.empty();
            }
            try {
                Object value = ReflectionUtils.invokeMethod(matched, tool, query);
                if (value instanceof GeoKnowledgeSummary summary) {
                    return Optional.of(summary);
                }
            }
            catch (Exception ex) {
                return Optional.of(GeoKnowledgeSummary.empty());
            }
            return Optional.empty();
        }

        private Method findToolMethod(Class<?> type) {
            Method cached = ReflectionUtils.findMethod(type, "lookup", String.class);
            if (cached != null && AnnotationUtils.findAnnotation(cached, GeoTool.class) != null) {
                return cached;
            }
            for (Method method : type.getMethods()) {
                GeoTool annotation = AnnotationUtils.findAnnotation(method, GeoTool.class);
                if (annotation != null && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == String.class) {
                    return method;
                }
            }
            return null;
        }

        private String buildContent(GeoKnowledgeSummary summary) {
            StringBuilder builder = new StringBuilder();
            String prefix = properties.getResponsePrefix();
            if (StringUtils.hasText(prefix)) {
                builder.append(prefix);
            }
            if (StringUtils.hasText(summary.title())) {
                builder.append(summary.title()).append(": ");
            }
            if (StringUtils.hasText(summary.summary())) {
                builder.append(summary.summary());
            }
            if (!summary.highlights().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append("Highlights:\n").append(summary.toBulletList());
            }
            if (builder.length() == 0) {
                builder.append("No information was found for the provided query.");
            }
            if (this.options == null || !StringUtils.hasText(this.options.getModel())) {
                return builder.toString();
            }
            return builder.append('\n')
                    .append("[model=")
                    .append(this.options.getModel())
                    .append(']')
                    .toString();
        }
    }

    private class GeoCallResponseSpec implements CallResponseSpec {

        private final String content;

        GeoCallResponseSpec(String content) {
            this.content = Objects.requireNonNullElse(content, "").trim();
        }

        @Override
        public <T> T entity(ParameterizedTypeReference<T> responseType) {
            throw new UnsupportedOperationException("Entity conversion is not supported by GeoChatClient.");
        }

        @Override
        public <T> T entity(StructuredOutputConverter<T> converter) {
            throw new UnsupportedOperationException("Entity conversion is not supported by GeoChatClient.");
        }

        @Override
        public <T> T entity(Class<T> responseType) {
            if (responseType.isAssignableFrom(String.class)) {
                return responseType.cast(content());
            }
            throw new UnsupportedOperationException("Only String responses are supported.");
        }

        @Override
        public org.springframework.ai.chat.client.ChatClientResponse chatClientResponse() {
            throw new UnsupportedOperationException("ChatClientResponse is not supported by GeoChatClient.");
        }

        @Override
        public ChatResponse chatResponse() {
            throw new UnsupportedOperationException("ChatResponse is not supported by GeoChatClient.");
        }

        @Override
        public String content() {
            return content;
        }

        @Override
        public <T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> responseBodyClass) {
            throw new UnsupportedOperationException("Response entities are not supported by GeoChatClient.");
        }

        @Override
        public <T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> responseBodyType) {
            throw new UnsupportedOperationException("Response entities are not supported by GeoChatClient.");
        }

        @Override
        public <T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> converter) {
            throw new UnsupportedOperationException("Response entities are not supported by GeoChatClient.");
        }
    }

    private class GeoChatClientBuilder implements Builder {

        private ChatOptions options;

        @Override
        public Builder defaultAdvisors(Advisor... advisors) {
            return this;
        }

        @Override
        public Builder defaultAdvisors(Consumer<AdvisorSpec> consumer) {
            return this;
        }

        @Override
        public Builder defaultAdvisors(List<Advisor> advisors) {
            return this;
        }

        @Override
        public Builder defaultOptions(ChatOptions options) {
            this.options = options;
            return this;
        }

        @Override
        public Builder defaultUser(String user) {
            return this;
        }

        @Override
        public Builder defaultUser(Resource resource, Charset charset) {
            return this;
        }

        @Override
        public Builder defaultUser(Resource resource) {
            return this;
        }

        @Override
        public Builder defaultUser(Consumer<PromptUserSpec> consumer) {
            return this;
        }

        @Override
        public Builder defaultSystem(String system) {
            return this;
        }

        @Override
        public Builder defaultSystem(Resource resource, Charset charset) {
            return this;
        }

        @Override
        public Builder defaultSystem(Resource resource) {
            return this;
        }

        @Override
        public Builder defaultSystem(Consumer<PromptSystemSpec> consumer) {
            return this;
        }

        @Override
        public Builder defaultTemplateRenderer(TemplateRenderer templateRenderer) {
            return this;
        }

        @Override
        public Builder defaultToolNames(String... toolNames) {
            return this;
        }

        @Override
        public Builder defaultTools(Object... tools) {
            return this;
        }

        @Override
        public Builder defaultToolCallbacks(org.springframework.ai.tool.ToolCallback... toolCallbacks) {
            return this;
        }

        @Override
        public Builder defaultToolCallbacks(List<org.springframework.ai.tool.ToolCallback> toolCallbacks) {
            return this;
        }

        @Override
        public Builder defaultToolCallbacks(org.springframework.ai.tool.ToolCallbackProvider... providers) {
            return this;
        }

        @Override
        public Builder defaultToolContext(Map<String, Object> toolContext) {
            return this;
        }

        @Override
        public Builder clone() {
            GeoChatClientBuilder clone = new GeoChatClientBuilder();
            clone.options = this.options;
            return clone;
        }

        @Override
        public ChatClient build() {
            ChatOptions effective = this.options;
            if (effective == null && StringUtils.hasText(properties.getModel())) {
                effective = ChatOptions.builder().model(properties.getModel()).build();
            }
            return new GeoChatClient(geoKnowledgeService, properties, effective);
        }
    }
}
