package org.wrj.haifa.ai.deerflow.skill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class SlashSkillResolverTest {

    @Test
    void resolvesSkillFromSlashCommand(@TempDir Path tmp) throws IOException {
        Path publicDir = tmp.resolve("public");
        Files.createDirectories(publicDir.resolve("research"));
        Files.writeString(publicDir.resolve("research").resolve("SKILL.md"), "# Research\nDo research.");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, null);
        SlashSkillResolver resolver = new SlashSkillResolver(storage);

        List<Skill> skills = resolver.resolve("/research summarize this repo");
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("research");
    }

    @Test
    void resolvesMultipleSkills(@TempDir Path tmp) throws IOException {
        Path publicDir = tmp.resolve("public");
        Files.createDirectories(publicDir.resolve("research"));
        Files.writeString(publicDir.resolve("research").resolve("SKILL.md"), "# Research");
        Files.createDirectories(publicDir.resolve("writer"));
        Files.writeString(publicDir.resolve("writer").resolve("SKILL.md"), "# Writer");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, null);
        SlashSkillResolver resolver = new SlashSkillResolver(storage);

        List<Skill> skills = resolver.resolve("/research and /writer do something");
        assertThat(skills).hasSize(2);
        assertThat(skills).extracting(Skill::name).containsExactly("research", "writer");
    }

    @Test
    void returnsEmptyForUnknownSkill(@TempDir Path tmp) throws IOException {
        Path publicDir = tmp.resolve("public");
        Files.createDirectories(publicDir.resolve("research"));
        Files.writeString(publicDir.resolve("research").resolve("SKILL.md"), "# Research");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, null);
        SlashSkillResolver resolver = new SlashSkillResolver(storage);

        List<Skill> skills = resolver.resolve("/unknown do something");
        assertThat(skills).isEmpty();
    }

    @Test
    void returnsEmptyForNoSlashCommand() {
        SlashSkillResolver resolver = new SlashSkillResolver(new FileSystemSkillStorage(null, null));
        assertThat(resolver.resolve("hello world")).isEmpty();
        assertThat(resolver.resolve("")).isEmpty();
        assertThat(resolver.resolve(null)).isEmpty();
    }
}
