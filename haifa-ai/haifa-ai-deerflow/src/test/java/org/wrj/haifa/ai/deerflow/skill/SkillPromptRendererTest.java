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

        assertThat(section).contains("<skill_system>");
        assertThat(section).contains("research");
        assertThat(section).contains("writer");
        assertThat(section).contains("SKILL CONTENT START: research");
    }

    @Test
    void returnsEmptyStringForNoSkills() {
        assertThat(SkillPromptRenderer.renderActiveSkills(List.of())).isEmpty();
        assertThat(SkillPromptRenderer.renderActiveSkills(null)).isEmpty();
    }

    @Test
    void injectsSkillsSectionIntoSystemPrompt() {
        String section = "<skill_system>active skills</skill_system>";
        String result = SkillPromptRenderer.injectIntoSystemPrompt("You are helpful.", section);

        assertThat(result).startsWith("You are helpful.");
        assertThat(result).contains("<skill_system>");
    }

    @Test
    void returnsSkillsSectionWhenSystemPromptIsNull() {
        String section = "<skill_system>active skills</skill_system>";
        String result = SkillPromptRenderer.injectIntoSystemPrompt(null, section);

        assertThat(result).isEqualTo("<skill_system>active skills</skill_system>");
    }

    @Test
    void returnsSystemPromptWhenSkillsSectionIsEmpty() {
        assertThat(SkillPromptRenderer.injectIntoSystemPrompt("base", "")).isEqualTo("base");
        assertThat(SkillPromptRenderer.injectIntoSystemPrompt("base", null)).isEqualTo("base");
    }
}
