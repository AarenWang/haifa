package org.wrj.haifa.ai.deerflow.skill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemSkillStorageTest {

    @Test
    void discoversPublicAndCustomSkills(@TempDir Path tmp) throws IOException {
        Path publicDir = tmp.resolve("public");
        Path customDir = tmp.resolve("custom");

        createSkill(publicDir, "research", "# Research Skill\nDo research.");
        createSkill(customDir, "my-plugin", "# My Plugin\nCustom plugin.");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, customDir);

        List<Skill> publicSkills = storage.listPublicSkills();
        List<Skill> customSkills = storage.listCustomSkills();

        assertThat(publicSkills).hasSize(1);
        assertThat(publicSkills.get(0).name()).isEqualTo("research");
        assertThat(customSkills).hasSize(1);
        assertThat(customSkills.get(0).name()).isEqualTo("my-plugin");
    }

    @Test
    void findAnyPrefersCustomOverPublic(@TempDir Path tmp) throws IOException {
        Path publicDir = tmp.resolve("public");
        Path customDir = tmp.resolve("custom");

        createSkill(publicDir, "shared", "# Public\nPublic version.");
        createSkill(customDir, "shared", "# Custom\nCustom version.");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, customDir);

        Optional<Skill> skill = storage.findAny("shared");
        assertThat(skill).isPresent();
        assertThat(skill.get().description()).isEqualTo("Custom");
        assertThat(skill.get().source()).isEqualTo("custom");
    }

    @Test
    void returnsEmptyForMissingSkill(@TempDir Path tmp) {
        FileSystemSkillStorage storage = new FileSystemSkillStorage(tmp.resolve("public"), tmp.resolve("custom"));
        assertThat(storage.findAny("missing")).isEmpty();
        assertThat(storage.listPublicSkills()).isEmpty();
        assertThat(storage.listCustomSkills()).isEmpty();
    }

    @Test
    void ignoresNonDirectories(@TempDir Path tmp) throws IOException {
        Path publicDir = tmp.resolve("public");
        Files.createDirectories(publicDir);
        Files.writeString(publicDir.resolve("not-a-dir"), "text");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, null);
        assertThat(storage.listPublicSkills()).isEmpty();
    }

    @Test
    void autoUnpacksPublicSkillsFromClasspath(@TempDir Path tmp) {
        org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties = new org.wrj.haifa.ai.deerflow.config.DeerFlowProperties();
        properties.setSkillsRoot(tmp.resolve("skills").toString());
        properties.setSkillsEnabled(true);

        FileSystemSkillStorage storage = new FileSystemSkillStorage(properties);
        List<Skill> publicSkills = storage.listPublicSkills();
        assertThat(publicSkills).isNotEmpty();
        assertThat(publicSkills).extracting(Skill::name).contains("deep-research");
    }

    @Test
    void bundledDeepResearchIncludesAllowedResearchTools(@TempDir Path tmp) {
        org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties = new org.wrj.haifa.ai.deerflow.config.DeerFlowProperties();
        properties.setSkillsRoot(tmp.resolve("skills").toString());
        properties.setSkillsEnabled(true);

        FileSystemSkillStorage storage = new FileSystemSkillStorage(properties);
        Skill skill = storage.findAny("deep-research").orElseThrow();

        assertThat(skill.allowedTools()).contains("web_search", "web_fetch", "image_search");
    }

    private static void createSkill(Path root, String name, String content) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("SKILL.md"), content);
    }
}
