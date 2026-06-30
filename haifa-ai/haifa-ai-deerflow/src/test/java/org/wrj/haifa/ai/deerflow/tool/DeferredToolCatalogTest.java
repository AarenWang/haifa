package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.skill.Skill;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredToolCatalogTest {

    private final AgentTool builtinA = new DummyTool("read_file", "Read a file");
    private final AgentTool builtinB = new DummyTool("list_files", "List files");
    private final Skill research = new Skill("research", "Do research", "public", "md", Map.of(), Set.of("web_search", "web_fetch"));
    private final Skill writer = new Skill("writer", "Write text", "custom", "md", Map.of(), Set.of("summarize"));

    private final DeferredToolCatalog catalog = new DeferredToolCatalog(
            List.of(builtinA, builtinB),
            List.of(research, writer)
    );

    @Test
    void searchFindsBuiltinTools() {
        List<ToolDescriptor> results = catalog.search("read");
        assertThat(results).extracting(ToolDescriptor::name).contains("read_file");
        assertThat(results).extracting(ToolDescriptor::source).contains("configured");
        assertThat(results).anySatisfy(t -> assertThat(t.requiresSkillActivation()).isFalse());
    }

    @Test
    void searchFindsSkillToolsByKeyword() {
        List<ToolDescriptor> results = catalog.search("web");
        assertThat(results).extracting(ToolDescriptor::name).contains("web_search", "web_fetch");
        assertThat(results).anySatisfy(t -> assertThat(t.source()).isEqualTo("configured"));
    }

    @Test
    void searchFindsSkillToolsBySkillName() {
        List<ToolDescriptor> results = catalog.search("writer");
        assertThat(results).extracting(ToolDescriptor::name).contains("summarize");
    }

    @Test
    void emptyKeywordListsAll() {
        List<ToolDescriptor> results = catalog.search("");
        assertThat(results.size()).isGreaterThanOrEqualTo(21); // standardDescriptors size + skills
    }

    @Test
    void listAllReturnsEverything() {
        List<ToolDescriptor> results = catalog.listAll();
        assertThat(results.size()).isGreaterThanOrEqualTo(21);
    }

    @Test
    void noMatchReturnsEmpty() {
        assertThat(catalog.search("zzzzzzzz")).isEmpty();
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
