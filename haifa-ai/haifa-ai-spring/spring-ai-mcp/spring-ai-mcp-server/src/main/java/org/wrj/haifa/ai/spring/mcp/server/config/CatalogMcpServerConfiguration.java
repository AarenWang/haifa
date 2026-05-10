package org.wrj.haifa.ai.spring.mcp.server.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wrj.haifa.ai.spring.mcp.server.service.CatalogInventory;
import org.wrj.haifa.ai.spring.mcp.server.service.CatalogTools;

import java.util.List;
import java.util.Map;

@Configuration
public class CatalogMcpServerConfiguration {

    private final ObjectMapper objectMapper;

    public CatalogMcpServerConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public ToolCallbackProvider catalogToolCallbackProvider(CatalogTools catalogTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(catalogTools)
                .build();
    }

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> catalogResources(CatalogInventory inventory) {
        McpSchema.Resource featuredCatalog = new McpSchema.Resource(
                "catalog://featured",
                "featured-catalog",
                "Featured catalog resource",
                "application/json",
                null);

        McpSchema.Resource onboardingPlaybook = new McpSchema.Resource(
                "catalog://playbook/onboarding",
                "catalog-onboarding-playbook",
                "MCP onboarding checklist resource",
                "application/json",
                null);

        return List.of(
                new McpServerFeatures.SyncResourceSpecification(featuredCatalog, (exchange, request) ->
                        new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        request.uri(),
                                        "application/json",
                                        toJson(inventory.featuredItems()))))),
                new McpServerFeatures.SyncResourceSpecification(onboardingPlaybook, (exchange, request) ->
                        new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        request.uri(),
                                        "application/json",
                                        toJson(inventory.onboardingPlaybook())))))
        );
    }

    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> catalogPrompts() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "sales-playbook",
                "Prompt template for a sales or shopping assistant",
                List.of(
                        new McpSchema.PromptArgument("audience", "Target audience", true),
                        new McpSchema.PromptArgument("goal", "Conversation goal", true)));

        return List.of(new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> arguments = request.arguments();
            String audience = stringArgument(arguments, "audience", "engineering-team");
            String goal = stringArgument(arguments, "goal", "complete-a-product-recommendation");
            String message = """
                    You are a presales assistant familiar with Spring AI and MCP.
                    Audience: %s
                    Goal: %s

                    Prioritize products related to MCP client and server development.
                    Include:
                    1. Why the recommendation fits
                    2. A sensible adoption order
                    3. First-run implementation cautions
                    """.formatted(audience, goal);

            return new McpSchema.GetPromptResult(
                    "Sales guidance prompt",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(message))));
        }));
    }

    private String stringArgument(Map<String, Object> arguments, String key, String defaultValue) {
        Object value = arguments.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize MCP resource payload", ex);
        }
    }
}
