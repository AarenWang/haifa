package org.wrj.haifa.ai.deerflow.skill;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillPromptRendererTest {

    @Test
    void rendersActiveSkillsWithAllowedTools() {
        Skill skill1 = new Skill("research", "Do research", "public", "content", Map.of(), Set.of("web_search", "summarize"));
        Skill skill2 = new Skill("writer", "Write text", "custom", "content", Map.of(), Set.of());

        String section = SkillPromptRenderer.renderActiveSkills(List.of(skill1, skill2));

        assertThat(section).contains("[Active skills]");
        assertThat(section).contains("- research: Do research");
        assertThat(section).contains("Allowed tools: web_search, summarize");
        assertThat(section).contains("- writer: Write text");
    }

    @Test
    void returnsEmptyStringForNoSkills() {
        assertThat(SkillPromptRenderer.renderActiveSkills(List.of())).isEmpty();
        assertThat(SkillPromptRenderer.renderActiveSkills(null)).isEmpty();
    }

    @Test
    void injectsSkillsSectionIntoSystemPrompt() {
        String section = "[Active skills]\n- research\n";
        String result = SkillPromptRenderer.injectIntoSystemPrompt("You are helpful.", section);

        assertThat(result).startsWith("You are helpful.");
        assertThat(result).contains("[Active skills]");
    }

    @Test
    void returnsSkillsSectionWhenSystemPromptIsNull() {
        String section = "[Active skills]\n- research\n";
        String result = SkillPromptRenderer.injectIntoSystemPrompt(null, section);

        assertThat(result).isEqualTo("[Active skills]\n- research");
    }

    @Test
    void returnsSystemPromptWhenSkillsSectionIsEmpty() {
        assertThat(SkillPromptRenderer.injectIntoSystemPrompt("base", "")).isEqualTo("base");
        assertThat(SkillPromptRenderer.injectIntoSystemPrompt("base", null)).isEqualTo("base");
    }
}
