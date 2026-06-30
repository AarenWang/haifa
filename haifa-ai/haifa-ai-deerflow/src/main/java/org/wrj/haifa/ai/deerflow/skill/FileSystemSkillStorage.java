package org.wrj.haifa.ai.deerflow.skill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class FileSystemSkillStorage implements SkillStorage {

    private static final Logger log = LoggerFactory.getLogger(FileSystemSkillStorage.class);

    private final Path publicRoot;
    private final Path customRoot;
    private final boolean enabled;

    public FileSystemSkillStorage(DeerFlowProperties properties) {
        this(Path.of(properties.getSkillsRoot()).resolve("public"),
                Path.of(properties.getSkillsRoot()).resolve("custom"),
                properties.isSkillsEnabled());
    }

    public FileSystemSkillStorage(Path publicRoot, Path customRoot) {
        this(publicRoot, customRoot, true);
    }

    public FileSystemSkillStorage(Path publicRoot, Path customRoot, boolean enabled) {
        this.publicRoot = publicRoot;
        this.customRoot = customRoot;
        this.enabled = enabled;
    }

    @Override
    public List<Skill> listPublicSkills() {
        if (!enabled) {
            return Collections.emptyList();
        }
        return listSkillsIn(publicRoot, "public");
    }

    @Override
    public List<Skill> listCustomSkills() {
        if (!enabled) {
            return Collections.emptyList();
        }
        return listSkillsIn(customRoot, "custom");
    }

    @Override
    public Optional<Skill> findPublic(String name) {
        if (!enabled) {
            return Optional.empty();
        }
        return findSkillIn(publicRoot, name, "public");
    }

    @Override
    public Optional<Skill> findCustom(String name) {
        if (!enabled) {
            return Optional.empty();
        }
        return findSkillIn(customRoot, name, "custom");
    }

    private List<Skill> listSkillsIn(Path root, String source) {
        if (root == null || !Files.isDirectory(root)) {
            return Collections.emptyList();
        }
        List<Skill> skills = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.toList()) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                Path skillMd = dir.resolve("SKILL.md");
                if (!Files.isRegularFile(skillMd)) {
                    continue;
                }
                try {
                    skills.add(SkillParser.parse(dir, source));
                } catch (IOException ex) {
                    log.warn("Failed to parse skill at {}", dir, ex);
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to list skills in {}", root, ex);
        }
        return skills;
    }

    private Optional<Skill> findSkillIn(Path root, String name, String source) {
        if (root == null || !Files.isDirectory(root)) {
            return Optional.empty();
        }
        Path dir = root.resolve(name);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        Path skillMd = dir.resolve("SKILL.md");
        if (!Files.isRegularFile(skillMd)) {
            return Optional.empty();
        }
        try {
            return Optional.of(SkillParser.parse(dir, source));
        } catch (IOException ex) {
            log.warn("Failed to parse skill {} at {}", name, dir, ex);
            return Optional.empty();
        }
    }
}
