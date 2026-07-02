package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.skill.FileSystemSkillStorage;
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

    @Test
    void standardToolsAreAllowedWithoutSkillActivation() {
        AgentTool webSearch = new DummyTool("web_search", "search");
        AgentTool writeFile = new DummyTool("write_file", "write");
        AgentTool current = new DummyTool("current_time", "time");

        ToolPolicyService localPolicy = new ToolPolicyService(List.of(webSearch, writeFile, current));

        // current_time is a standard built-in, so it is always allowed
        assertThat(localPolicy.isToolAllowed("current_time", List.of())).isTrue();

        // web_search and write_file are standard configured tools, allowed without skill activation
        assertThat(localPolicy.isToolAllowed("web_search", List.of())).isTrue();
        assertThat(localPolicy.isToolAllowed("write_file", List.of())).isTrue();

        // Still allowed if a skill explicitly mentions them
        Skill skill = new Skill("research", "Do research", "public", "md", Map.of(), Set.of("web_search"));
        assertThat(localPolicy.isToolAllowed("web_search", List.of(skill))).isTrue();
        assertThat(localPolicy.isToolAllowed("write_file", List.of(skill))).isTrue();
    }

    @Test
    void bundledDeepResearchAllowsWebResearchTools(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSkillsRoot(tmp.resolve("skills").toString());
        properties.setSkillsEnabled(true);

        FileSystemSkillStorage storage = new FileSystemSkillStorage(properties);
        Skill deepResearch = storage.findAny("deep-research").orElseThrow();

        AgentTool webSearch = new DummyTool("web_search", "search");
        AgentTool webFetch = new DummyTool("web_fetch", "fetch");
        AgentTool imageSearch = new DummyTool("image_search", "images");
        AgentTool current = new DummyTool("current_time", "time");

        ToolPolicyService localPolicy = new ToolPolicyService(List.of(webSearch, webFetch, imageSearch, current));

        assertThat(localPolicy.isToolAllowed("web_search", List.of(deepResearch))).isTrue();
        assertThat(localPolicy.isToolAllowed("web_fetch", List.of(deepResearch))).isTrue();
        assertThat(localPolicy.isToolAllowed("image_search", List.of(deepResearch))).isTrue();
    }

    @Test
    void runScriptRequiresSkillInAllModes() {
        AgentTool runScript = new DummyTool("run_script", "run script");
        ToolPolicyService localPolicy = new ToolPolicyService(List.of(runScript));

        // CHAT mode, no active skills => disallowed
        assertThat(localPolicy.isToolAllowed("run_script", List.of(), org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT)).isFalse();

        // CHAT mode, with local-script-execution skill => allowed
        Skill skill = new Skill("local-script-execution", "Execute script", "public", "md", Map.of(), Set.of("run_script"));
        assertThat(localPolicy.isToolAllowed("run_script", List.of(skill), org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT)).isTrue();

        // RESEARCH mode, no active skills => disallowed
        assertThat(localPolicy.isToolAllowed("run_script", List.of(), org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH)).isFalse();

        // RESEARCH mode, with skill => allowed
        assertThat(localPolicy.isToolAllowed("run_script", List.of(skill), org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH)).isTrue();
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
