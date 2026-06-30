package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ToolSearchTool implements AgentTool {

    private final DeferredToolCatalog catalog;
    private final boolean enabled;

    public ToolSearchTool(DeferredToolCatalog catalog, org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties) {
        this.catalog = catalog;
        this.enabled = properties.isToolSearchEnabled();
    }

    @Override
    public String name() {
        return "tool_search";
    }

    @Override
    public String description() {
        return "Search available tools and skills by keyword. Returns tool name, description, source, and whether skill activation is required.";
    }

    @Override
    public boolean supports(String userMessage) {
        if (!enabled || userMessage == null) {
            return false;
        }
        String text = userMessage.toLowerCase();
        return text.contains("tool_search")
                || text.contains("search tool")
                || text.contains("find tool")
                || text.contains("list tools")
                || text.contains("what tools")
                || text.contains("available tools");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String keyword = extractKeyword(request.userMessage());
        List<ToolDescriptor> results = keyword.isBlank() ? catalog.listAll() : catalog.search(keyword);
        if (results.isEmpty()) {
            return ToolResult.of(name(), "No tools found matching '" + keyword + "'.");
        }
        String content = results.stream()
                .map(t -> "- " + t.name() + " (source: " + t.source() + ")" + (t.requiresSkillActivation() ? " [requires skill]" : "")
                        + "\n  " + t.description())
                .collect(Collectors.joining("\n\n", "Available tools:\n\n", ""));
        return ToolResult.of(name(), content);
    }

    private static String extractKeyword(String userMessage) {
        if (userMessage == null) {
            return "";
        }
        String[] prefixes = {"tool_search", "search tool", "find tool", "list tools", "what tools", "available tools"};
        String lower = userMessage.toLowerCase();
        for (String prefix : prefixes) {
            int idx = lower.indexOf(prefix);
            if (idx >= 0) {
                String after = userMessage.substring(idx + prefix.length()).trim();
                after = after.replaceFirst("^[\\p{Punct}\\s]+", "");
                return after;
            }
        }
        return userMessage;
    }
}
