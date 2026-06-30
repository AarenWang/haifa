package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.skill.Skill;

import static org.assertj.core.api.Assertions.assertThat;

class ToolPolicyServiceTest {

    private final AgentTool builtinA = new DummyTool("builtin_a", "Built-in A");
    private final AgentTool builtinB = new DummyTool("builtin_b", "Built-in B");
    private final ToolPolicyService policy = new ToolPolicyService(List.of(builtinA, builtinB));

    @Test
    void builtinToolsAreAlwaysAllowed() {
        assertThat(policy.isToolAllowed("builtin_a", List.of())).isTrue();
        assertThat(policy.isToolAllowed("builtin_b", null)).isTrue();
    }

    @Test
    void unknownToolWithoutSkillIsDisallowed() {
        assertThat(policy.isToolAllowed("mystery_tool", List.of())).isFalse();
        assertThat(policy.isToolAllowed("mystery_tool", null)).isFalse();
    }

    @Test
    void skillAllowsListedTools() {
        Skill skill = new Skill("research", "Do research", "public", "md", Map.of(), Set.of("web_search", "summarize"));
        assertThat(policy.isToolAllowed("web_search", List.of(skill))).isTrue();
        assertThat(policy.isToolAllowed("summarize", List.of(skill))).isTrue();
        assertThat(policy.isToolAllowed("unknown_tool", List.of(skill))).isFalse();
    }

    @Test
    void nullOrBlankToolNameIsDisallowed() {
        assertThat(policy.isToolAllowed(null, List.of())).isFalse();
        assertThat(policy.isToolAllowed("", List.of())).isFalse();
        assertThat(policy.isToolAllowed("  ", List.of())).isFalse();
    }

    @Test
    void allowedToolsForSkillsIncludesBuiltinAndSkillTools() {
        Skill skill = new Skill("research", "Do research", "public", "md", Map.of(), Set.of("web_search"));
        Set<String> allowed = policy.allowedToolsForSkills(List.of(skill));
        assertThat(allowed).containsExactlyInAnyOrder("builtin_a", "builtin_b", "web_search");
    }

    @Test
    void allowedToolsForSkillsWithEmptySkillsReturnsOnlyBuiltin() {
        Set<String> allowed = policy.allowedToolsForSkills(List.of());
        assertThat(allowed).containsExactlyInAnyOrder("builtin_a", "builtin_b");
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
