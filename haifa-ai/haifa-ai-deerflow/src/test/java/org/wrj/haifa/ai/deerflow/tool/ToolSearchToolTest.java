package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.skill.Skill;

import static org.assertj.core.api.Assertions.assertThat;

class ToolSearchToolTest {

    private final AgentTool builtinA = new DummyTool("read_file", "Read a file");
    private final Skill research = new Skill("research", "Do research", "public", "md", Map.of(), Set.of("web_search"));
    private final DeferredToolCatalog catalog = new DeferredToolCatalog(List.of(builtinA), List.of(research));
    private final ToolSearchTool searchTool = new ToolSearchTool(catalog, new org.wrj.haifa.ai.deerflow.config.DeerFlowProperties());

    @Test
    void supportsToolSearchQueries() {
        assertThat(searchTool.supports("search tool read")).isTrue();
        assertThat(searchTool.supports("tool_search web")).isTrue();
        assertThat(searchTool.supports("find tool something")).isTrue();
        assertThat(searchTool.supports("list tools")).isTrue();
        assertThat(searchTool.supports("what tools can I use")).isTrue();
        assertThat(searchTool.supports("请告诉我目前的智能体有多少工具可以使用")).isTrue();
        assertThat(searchTool.supports("hello world")).isFalse();
    }

    @Test
    void returnsSearchResults() {
        ToolRequest request = new ToolRequest("search tool read", java.nio.file.Path.of("."));
        ToolResult result = searchTool.execute(request);
        assertThat(result.content()).contains("read_file");
        assertThat(result.content()).contains("configured");
    }

    @Test
    void returnsAllToolsForEmptyKeyword() {
        ToolRequest request = new ToolRequest("list tools", java.nio.file.Path.of("."));
        ToolResult result = searchTool.execute(request);
        assertThat(result.content()).contains("Available tools (22 total):");
        assertThat(result.content()).contains("read_file");
        assertThat(result.content()).contains("web_search");
        assertThat(result.content()).doesNotContain("[requires skill]", "Provided by skill");
    }

    @Test
    void returnsAllToolsForChineseInventoryQuestion() {
        ToolRequest request = new ToolRequest("请告诉我目前的智能体有多少工具可以使用", java.nio.file.Path.of("."));
        ToolResult result = searchTool.execute(request);
        assertThat(result.content()).contains("Available tools (22 total):");
        assertThat(result.content()).contains("read_file");
        assertThat(result.content()).contains("web_search");
    }

    @Test
    void returnsNoMatchMessage() {
        ToolRequest request = new ToolRequest("search tool zzzzzz", java.nio.file.Path.of("."));
        ToolResult result = searchTool.execute(request);
        assertThat(result.content()).contains("No tools found");
    }

    private static final class DummyTool implements AgentTool {
        private final String name;
        private final String description;

        DummyTool(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String name() { return name; }

        @Override
        public String description() { return description; }

        @Override
        public boolean supports(String userMessage) { return false; }

        @Override
        public ToolResult execute(ToolRequest request) { return ToolResult.of(name, "done"); }
    }
}
