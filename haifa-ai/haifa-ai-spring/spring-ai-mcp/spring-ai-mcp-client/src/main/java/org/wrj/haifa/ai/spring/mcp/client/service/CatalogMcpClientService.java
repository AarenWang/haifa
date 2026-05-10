package org.wrj.haifa.ai.spring.mcp.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogMcpClientService {

    private final List<McpSyncClient> clients;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;

    public CatalogMcpClientService(
            List<McpSyncClient> clients,
            ObjectProvider<ToolCallbackProvider> toolCallbackProvider,
            ObjectMapper objectMapper) {
        this.clients = clients;
        this.toolCallbackProvider = toolCallbackProvider.getIfAvailable(() -> ToolCallbackProvider.from(new ToolCallback[0]));
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> overview() {
        return Map.of(
                "clientCount", clients.size(),
                "servers", clients.stream()
                        .map(client -> Map.of(
                                "name", client.getServerInfo().name(),
                                "version", client.getServerInfo().version()))
                        .toList(),
                "capabilities", capabilities(),
                "toolCallbackBridgeEnabled", toolCallbackProvider.getToolCallbacks().length > 0,
                "springAiToolCallbacks", Arrays.stream(toolCallbackProvider.getToolCallbacks())
                        .map(tool -> Map.of(
                                "name", tool.getToolDefinition().name(),
                                "description", tool.getToolDefinition().description()))
                        .toList());
    }

    public Map<String, Object> capabilities() {
        McpSchema.ListToolsResult tools = listTools();
        McpSchema.ListResourcesResult resources = listResources();
        McpSchema.ListPromptsResult prompts = listPrompts();

        return Map.of(
                "toolCount", tools.tools().size(),
                "resourceCount", resources.resources().size(),
                "promptCount", prompts.prompts().size(),
                "tools", tools.tools().stream()
                        .map(tool -> Map.of(
                                "name", tool.name(),
                                "description", tool.description()))
                        .toList(),
                "resources", resources.resources().stream()
                        .map(resource -> Map.of(
                                "uri", resource.uri(),
                                "name", resource.name(),
                                "description", resource.description()))
                        .toList(),
                "prompts", prompts.prompts().stream()
                        .map(prompt -> Map.of(
                                "name", prompt.name(),
                                "description", prompt.description()))
                        .toList());
    }

    public McpSchema.ListToolsResult listTools() {
        return primaryClient().listTools();
    }

    public McpSchema.CallToolResult searchCatalog(String scenario, Integer limit) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("scenario", scenario);
        if (limit != null) {
            arguments.put("limit", limit);
        }
        return primaryClient().callTool(new McpSchema.CallToolRequest("searchCatalog", arguments));
    }

    public Object searchCatalogViaSpringAiBridge(String scenario, Integer limit) {
        ToolCallback callback = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .filter(tool -> "searchCatalog".equals(tool.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("searchCatalog tool callback not found"));

        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("scenario", scenario);
        if (limit != null) {
            arguments.put("limit", limit);
        }

        try {
            String response = callback.call(objectMapper.writeValueAsString(arguments));
            return objectMapper.readTree(response);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to call MCP tool through Spring AI bridge", ex);
        }
    }

    public McpSchema.ListResourcesResult listResources() {
        return primaryClient().listResources();
    }

    public McpSchema.ReadResourceResult readFeaturedCatalog() {
        return primaryClient().readResource(new McpSchema.ReadResourceRequest("catalog://featured"));
    }

    public McpSchema.ReadResourceResult readOnboardingPlaybook() {
        return primaryClient().readResource(new McpSchema.ReadResourceRequest("catalog://playbook/onboarding"));
    }

    public McpSchema.ListPromptsResult listPrompts() {
        return primaryClient().listPrompts();
    }

    public McpSchema.GetPromptResult getSalesPlaybook(String audience, String goal) {
        Map<String, Object> arguments = Map.of(
                "audience", audience,
                "goal", goal);
        return primaryClient().getPrompt(new McpSchema.GetPromptRequest("sales-playbook", arguments));
    }

    public Map<String, Object> runExampleToolCall() {
        String scenario = "mcp starter for ecommerce assistant";
        Integer limit = 3;
        McpSchema.CallToolResult toolResult = searchCatalog(scenario, limit);

        return Map.of(
                "exampleName", "example-1-tool-call",
                "description", "Call the searchCatalog MCP tool directly from the client.",
                "request", Map.of(
                        "tool", "searchCatalog",
                        "arguments", Map.of(
                                "scenario", scenario,
                                "limit", limit)),
                "response", Map.of(
                        "raw", toolResult,
                        "parsedContent", extractToolContent(toolResult)));
    }

    public Map<String, Object> runExampleResourceAndPrompt() {
        McpSchema.ReadResourceResult onboardingResource = readOnboardingPlaybook();
        McpSchema.GetPromptResult promptResult = getSalesPlaybook(
                "new-user",
                "recommend an MCP starter package");

        return Map.of(
                "exampleName", "example-2-resource-and-prompt",
                "description", "Read an MCP resource first and then fetch a prompt template for the next conversation step.",
                "resourceStep", Map.of(
                        "uri", "catalog://playbook/onboarding",
                        "raw", onboardingResource,
                        "parsedContent", extractResourceContent(onboardingResource)),
                "promptStep", Map.of(
                        "name", "sales-playbook",
                        "arguments", Map.of(
                                "audience", "new-user",
                                "goal", "recommend an MCP starter package"),
                        "raw", promptResult,
                        "messages", extractPromptMessages(promptResult)));
    }

    public Map<String, Object> runCatalogAssistantDemo(String scenario, Integer limit, String audience, String goal) {
        McpSchema.CallToolResult directToolCall = searchCatalog(scenario, limit);
        Object bridgeToolCall = null;
        if (toolCallbackProvider.getToolCallbacks().length > 0) {
            bridgeToolCall = searchCatalogViaSpringAiBridge(scenario, limit);
        }

        McpSchema.ReadResourceResult featuredCatalog = readFeaturedCatalog();
        McpSchema.ReadResourceResult onboardingPlaybook = readOnboardingPlaybook();
        McpSchema.GetPromptResult salesPlaybook = getSalesPlaybook(audience, goal);

        return Map.of(
                "server", Map.of(
                        "name", primaryClient().getServerInfo().name(),
                        "version", primaryClient().getServerInfo().version()),
                "request", Map.of(
                        "scenario", scenario,
                        "limit", limit,
                        "audience", audience,
                        "goal", goal),
                "toolCall", Map.of(
                        "raw", directToolCall,
                        "parsedContent", extractToolContent(directToolCall)),
                "bridgeToolCall", bridgeToolCall == null ? "ToolCallback bridge is not available" : bridgeToolCall,
                "featuredCatalog", Map.of(
                        "raw", featuredCatalog,
                        "parsedContent", extractResourceContent(featuredCatalog)),
                "onboardingPlaybook", Map.of(
                        "raw", onboardingPlaybook,
                        "parsedContent", extractResourceContent(onboardingPlaybook)),
                "salesPlaybookPrompt", Map.of(
                        "raw", salesPlaybook,
                        "messages", extractPromptMessages(salesPlaybook)));
    }

    private McpSyncClient primaryClient() {
        if (clients.isEmpty()) {
            throw new IllegalStateException("No MCP client connection is available");
        }
        return clients.get(0);
    }

    private Object extractToolContent(McpSchema.CallToolResult result) {
        return result.content().stream()
                .map(content -> {
                    if (content instanceof McpSchema.TextContent textContent) {
                        return tryParseJson(textContent.text());
                    }
                    return content.toString();
                })
                .toList();
    }

    private Object extractResourceContent(McpSchema.ReadResourceResult result) {
        return result.contents().stream()
                .map(content -> {
                    if (content instanceof McpSchema.TextResourceContents textResourceContents) {
                        return Map.of(
                                "uri", textResourceContents.uri(),
                                "mimeType", textResourceContents.mimeType(),
                                "content", tryParseJson(textResourceContents.text()));
                    }
                    return content.toString();
                })
                .toList();
    }

    private Object extractPromptMessages(McpSchema.GetPromptResult result) {
        return result.messages().stream()
                .map(message -> {
                    Object content = message.content();
                    if (content instanceof McpSchema.TextContent textContent) {
                        return Map.of(
                                "role", message.role(),
                                "text", textContent.text());
                    }
                    return Map.of(
                            "role", message.role(),
                            "content", content.toString());
                })
                .toList();
    }

    private Object tryParseJson(String value) {
        try {
            return objectMapper.readTree(value);
        }
        catch (JsonProcessingException ex) {
            return value;
        }
    }
}
