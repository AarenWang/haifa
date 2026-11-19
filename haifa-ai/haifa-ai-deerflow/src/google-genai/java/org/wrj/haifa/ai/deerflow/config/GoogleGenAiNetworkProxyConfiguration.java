package org.wrj.haifa.ai.deerflow.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.ProxyOptions;
import com.google.genai.types.ProxyType;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/** Google GenAI SDK proxy wiring, compiled only by the google-genai Maven profile. */
@Configuration(proxyBeanMethods = false)
@Conditional(GoogleGenAiNetworkProxyConfiguration.NetworkProxyConfiguredCondition.class)
public class GoogleGenAiNetworkProxyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GoogleGenAiNetworkProxyConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(Client.class)
    Client googleGenAiClient(GoogleGenAiConnectionProperties connectionProperties,
            DeerFlowProperties deerFlowProperties) throws IOException {
        LlmNetworkProxyConfiguration.ProxySettings proxy =
                LlmNetworkProxyConfiguration.proxySettings(deerFlowProperties);
        Assert.notNull(proxy, "LLM network proxy must be configured");

        Client.Builder builder = configuredBuilder(connectionProperties);
        ProxyType.Known proxyType = proxyTypeFor(proxy.scheme());
        ProxyOptions.Builder proxyBuilder = ProxyOptions.builder()
                .host(proxy.host())
                .port(proxy.port())
                .type(proxyType);
        if (StringUtils.hasText(proxy.username())) {
            proxyBuilder.username(proxy.username()).password(proxy.password() == null ? "" : proxy.password());
        }
        builder.clientOptions(ClientOptions.builder().proxyOptions(proxyBuilder.build()).build());
        log.info("Configuring Google GenAI SDK {} proxy. proxy={}:{}",
                proxy.scheme(), proxy.host(), proxy.port());
        return builder.build();
    }

    static ProxyType.Known proxyTypeFor(String scheme) {
        return "socks5".equalsIgnoreCase(scheme) ? ProxyType.Known.SOCKS : ProxyType.Known.HTTP;
    }

    private static Client.Builder configuredBuilder(GoogleGenAiConnectionProperties properties) throws IOException {
        boolean apiKeyConfigured = StringUtils.hasText(properties.getApiKey());
        boolean vertexConfigured = StringUtils.hasText(properties.getProjectId())
                && StringUtils.hasText(properties.getLocation());
        boolean useVertex = properties.isVertexAi() || (!apiKeyConfigured && vertexConfigured);

        Client.Builder builder = Client.builder();
        if (useVertex) {
            Assert.isTrue(vertexConfigured,
                    "Vertex AI mode requires both 'project-id' and 'location' to be configured");
            builder.project(properties.getProjectId())
                    .location(properties.getLocation())
                    .vertexAI(true);
            if (properties.getCredentialsUri() != null) {
                try (InputStream input = properties.getCredentialsUri().getInputStream()) {
                    builder.credentials(GoogleCredentials.fromStream(input));
                }
            }
        }
        else if (apiKeyConfigured) {
            builder.apiKey(properties.getApiKey());
        }
        else {
            throw new IllegalStateException(
                    "Incomplete Google GenAI configuration: provide an API key or Vertex AI project and location");
        }
        return builder;
    }

    static final class NetworkProxyConfiguredCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return StringUtils.hasText(
                    context.getEnvironment().getProperty("haifa.ai.deerflow.network-proxy-url"));
        }
    }
}
