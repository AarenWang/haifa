package org.wrj.haifa.ai.deerflow.skill;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Skill(
        String name,
        String description,
        String source,
        String skillMdContent,
        Map<String, List<String>> directories,
        Set<String> allowedTools
) {

    public boolean hasReferences() {
        return hasDir("references");
    }

    public boolean hasTemplates() {
        return hasDir("templates");
    }

    public boolean hasScripts() {
        return hasDir("scripts");
    }

    public boolean hasAssets() {
        return hasDir("assets");
    }

    private boolean hasDir(String dir) {
        return directories != null && directories.containsKey(dir) && !directories.get(dir).isEmpty();
    }
}
