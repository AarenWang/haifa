package org.wrj.haifa.ai.spring.mcp.client.web;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.ai.spring.mcp.client.service.CatalogMcpClientService;

import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpClientDemoController {

    private final CatalogMcpClientService catalogMcpClientService;

    public McpClientDemoController(CatalogMcpClientService catalogMcpClientService) {
        this.catalogMcpClientService = catalogMcpClientService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return catalogMcpClientService.overview();
    }

    @GetMapping("/capabilities")
    public Map<String, Object> capabilities() {
        return catalogMcpClientService.capabilities();
    }

    @GetMapping("/examples/tool-call")
    public Map<String, Object> exampleToolCall() {
        return catalogMcpClientService.runExampleToolCall();
    }

    @GetMapping("/examples/resource-and-prompt")
    public Map<String, Object> exampleResourceAndPrompt() {
        return catalogMcpClientService.runExampleResourceAndPrompt();
    }

    @GetMapping("/tools")
    public McpSchema.ListToolsResult listTools() {
        return catalogMcpClientService.listTools();
    }

    @PostMapping("/tools/search-catalog")
    public McpSchema.CallToolResult searchCatalog(@RequestBody CatalogSearchRequest request) {
        return catalogMcpClientService.searchCatalog(request.scenario(), request.limit());
    }

    @PostMapping("/tools/search-catalog/bridge")
    public Object searchCatalogViaSpringAiBridge(@RequestBody CatalogSearchRequest request) {
        return catalogMcpClientService.searchCatalogViaSpringAiBridge(request.scenario(), request.limit());
    }

    @GetMapping("/resources")
    public McpSchema.ListResourcesResult listResources() {
        return catalogMcpClientService.listResources();
    }

    @GetMapping("/resources/featured")
    public McpSchema.ReadResourceResult readFeaturedCatalog() {
        return catalogMcpClientService.readFeaturedCatalog();
    }

    @GetMapping("/resources/onboarding")
    public McpSchema.ReadResourceResult readOnboardingPlaybook() {
        return catalogMcpClientService.readOnboardingPlaybook();
    }

    @GetMapping("/prompts")
    public McpSchema.ListPromptsResult listPrompts() {
        return catalogMcpClientService.listPrompts();
    }

    @GetMapping("/prompts/sales-playbook")
    public McpSchema.GetPromptResult getSalesPlaybook(
            @RequestParam(defaultValue = "new-user") String audience,
            @RequestParam(defaultValue = "first-order-conversion") String goal) {
        return catalogMcpClientService.getSalesPlaybook(audience, goal);
    }

    @PostMapping("/demo/catalog-assistant")
    public Map<String, Object> runCatalogAssistantDemo(@RequestBody CatalogAssistantRequest request) {
        String scenario = request.scenario() == null || request.scenario().isBlank()
                ? "mcp client demo"
                : request.scenario();
        String audience = request.audience() == null || request.audience().isBlank()
                ? "new-user"
                : request.audience();
        String goal = request.goal() == null || request.goal().isBlank()
                ? "first-order-conversion"
                : request.goal();
        return catalogMcpClientService.runCatalogAssistantDemo(
                scenario,
                request.limit(),
                audience,
                goal);
    }
}
