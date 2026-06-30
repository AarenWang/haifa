package org.wrj.haifa.ai.deerflow.skill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SkillParser {

    private static final Pattern H1_PATTERN = Pattern.compile("^#\\s+(.+)");
    private static final Pattern ALLOWED_TOOLS_SECTION = Pattern.compile("(?i)^#{1,3}\\s*allowed\\s*tools\\s*$");
    private static final Pattern ALLOWED_TOOLS_INLINE = Pattern.compile("(?i)^[-*]\\s*allowed-tools?\\s*:\\s*(.+)$");
    private static final Pattern LIST_ITEM = Pattern.compile("^[-*]\\s+(.+)$");
    private static final List<String> SUBDIRS = List.of("references", "templates", "scripts", "assets");

    private SkillParser() {
    }

    public static Skill parse(Path skillDir, String source) throws IOException {
        Path skillMd = skillDir.resolve("SKILL.md");
        String content = Files.readString(skillMd, StandardCharsets.UTF_8);
        String name = skillDir.getFileName().toString();
        String description = extractDescription(content);
        Map<String, List<String>> directories = scanSubdirectories(skillDir);
        Set<String> allowedTools = extractAllowedTools(content);
        return new Skill(name, description, source, content, directories, allowedTools);
    }

    private static String extractDescription(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String[] lines = content.split("\r?\n");
        for (String line : lines) {
            Matcher m = H1_PATTERN.matcher(line.trim());
            if (m.matches()) {
                return m.group(1).trim();
            }
        }
        // fallback: first non-empty line, limited to 120 chars
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.length() > 120) {
                    return trimmed.substring(0, 120) + "...";
                }
                return trimmed;
            }
        }
        return "";
    }

    private static Map<String, List<String>> scanSubdirectories(Path skillDir) {
        return SUBDIRS.stream()
                .collect(Collectors.toMap(
                        dir -> dir,
                        dir -> listFiles(skillDir.resolve(dir)),
                        (a, b) -> a
                ));
    }

    private static List<String> listFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            return List.of();
        }
    }

    private static Set<String> extractAllowedTools(String content) {
        Set<String> tools = new java.util.HashSet<>();
        String[] lines = content.split("\r?\n");
        boolean inSection = false;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            // Check for "## Allowed tools" section header
            if (ALLOWED_TOOLS_SECTION.matcher(line).matches()) {
                inSection = true;
                continue;
            }
            if (inSection) {
                // New markdown heading ends the section
                if (line.startsWith("#")) {
                    inSection = false;
                } else {
                    Matcher listMatcher = LIST_ITEM.matcher(line);
                    if (listMatcher.matches()) {
                        String item = listMatcher.group(1).trim();
                        if (!item.isEmpty()) {
                            tools.add(item);
                        }
                        continue;
                    } else {
                        inSection = false;
                    }
                }
            }
            // Check inline format: - allowed-tools: tool1, tool2
            Matcher inlineMatcher = ALLOWED_TOOLS_INLINE.matcher(line);
            if (inlineMatcher.matches()) {
                String csv = inlineMatcher.group(1);
                for (String t : csv.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) {
                        tools.add(trimmed);
                    }
                }
            }
        }
        return Set.copyOf(tools);
    }
}
