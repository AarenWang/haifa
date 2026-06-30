package org.wrj.haifa.ai.deerflow.skill;

import java.util.List;

public final class SkillPromptRenderer {

    private SkillPromptRenderer() {
    }

    public static String renderActiveSkills(List<Skill> activeSkills) {
        if (activeSkills == null || activeSkills.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n[Active skills]\n");
        for (Skill skill : activeSkills) {
            sb.append("- ").append(skill.name());
            if (skill.description() != null && !skill.description().isBlank()) {
                sb.append(": ").append(skill.description());
            }
            sb.append("\n");
            if (skill.allowedTools() != null && !skill.allowedTools().isEmpty()) {
                sb.append("  Allowed tools: ").append(String.join(", ", skill.allowedTools())).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String injectIntoSystemPrompt(String systemPrompt, String skillsSection) {
        if (skillsSection == null || skillsSection.isBlank()) {
            return systemPrompt == null ? "" : systemPrompt;
        }
        String base = systemPrompt == null ? "" : systemPrompt;
        if (base.isBlank()) {
            return skillsSection.trim();
        }
        return base + "\n\n" + skillsSection.trim();
    }
}
