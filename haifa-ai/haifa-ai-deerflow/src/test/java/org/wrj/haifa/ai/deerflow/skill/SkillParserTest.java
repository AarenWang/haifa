package org.wrj.haifa.ai.deerflow.skill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class SkillParserTest {

    @Test
    void parsesSkillFromDirectory(@TempDir Path tmp) throws IOException {
        Path skillDir = tmp.resolve("research");
        Files.createDirectories(skillDir);
        String md = """
                # Research Assistant
                Help the user perform research tasks.

                ## Allowed tools
                - web_search
                - web_fetch
                - allowed-tools: summarize
                """;
        Files.writeString(skillDir.resolve("SKILL.md"), md);
        Files.createDirectories(skillDir.resolve("references"));
        Files.writeString(skillDir.resolve("references").resolve("guide.md"), "guide");
        Files.createDirectories(skillDir.resolve("templates"));
        Files.writeString(skillDir.resolve("templates").resolve("template.md"), "template");

        Skill skill = SkillParser.parse(skillDir, "public");

        assertThat(skill.name()).isEqualTo("research");
        assertThat(skill.description()).isEqualTo("Research Assistant");
        assertThat(skill.source()).isEqualTo("public");
        assertThat(skill.skillMdContent()).isEqualTo(md);
        assertThat(skill.hasReferences()).isTrue();
        assertThat(skill.hasTemplates()).isTrue();
        assertThat(skill.hasScripts()).isFalse();
        assertThat(skill.hasAssets()).isFalse();
        assertThat(skill.allowedTools()).containsExactlyInAnyOrder("web_search", "web_fetch", "summarize");
    }

    @Test
    void parsesSkillWithYamlFrontMatter(@TempDir Path tmp) throws IOException {
        Path skillDir = tmp.resolve("yaml-skill");
        Files.createDirectories(skillDir);
        String md = """
                ---
                name: custom-deep-research
                description: Custom description for research.
                allowed-tools: [web_search, web_fetch, present_files]
                ---
                
                # Title
                Body text.
                """;
        Files.writeString(skillDir.resolve("SKILL.md"), md);

        Skill skill = SkillParser.parse(skillDir, "public");
        assertThat(skill.name()).isEqualTo("custom-deep-research");
        assertThat(skill.description()).isEqualTo("Custom description for research.");
        assertThat(skill.allowedTools()).containsExactlyInAnyOrder("web_search", "web_fetch", "present_files");
    }

    @Test
    void fallsBackToFirstLineWhenNoH1(@TempDir Path tmp) throws IOException {
        Path skillDir = tmp.resolve("plain");
        Files.createDirectories(skillDir);
        String md = "This is a plain skill description without markdown heading.";
        Files.writeString(skillDir.resolve("SKILL.md"), md);

        Skill skill = SkillParser.parse(skillDir, "custom");

        assertThat(skill.name()).isEqualTo("plain");
        assertThat(skill.description()).isEqualTo("This is a plain skill description without markdown heading.");
    }

    @Test
    void truncatesLongFirstLine(@TempDir Path tmp) throws IOException {
        Path skillDir = tmp.resolve("long");
        Files.createDirectories(skillDir);
        String longLine = "a".repeat(200);
        Files.writeString(skillDir.resolve("SKILL.md"), longLine);

        Skill skill = SkillParser.parse(skillDir, "custom");

        assertThat(skill.description()).hasSize(123).endsWith("...");
    }

    @Test
    void emptySkillMdReturnsEmptyDescription(@TempDir Path tmp) throws IOException {
        Path skillDir = tmp.resolve("empty");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), "");

        Skill skill = SkillParser.parse(skillDir, "public");

        assertThat(skill.description()).isEmpty();
        assertThat(skill.allowedTools()).isEmpty();
    }

    @Test
    void extractsAllowedToolsMultipleLines(@TempDir Path tmp) throws IOException {
        Path skillDir = tmp.resolve("multi-tools");
        Files.createDirectories(skillDir);
        String md = """
                # Multi Tool Skill
                Some description.

                - allowed-tools: tool_a, tool_b
                - allowed-tools: tool_c
                """;
        Files.writeString(skillDir.resolve("SKILL.md"), md);

        Skill skill = SkillParser.parse(skillDir, "custom");

        assertThat(skill.allowedTools()).containsExactlyInAnyOrder("tool_a", "tool_b", "tool_c");
    }
}
