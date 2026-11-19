package org.wrj.haifa.ai.deerflow.skill;

import java.util.List;

public final class SkillPromptRenderer {

    private SkillPromptRenderer() {
    }

    public static String renderActiveSkills(List<Skill> activeSkills) {
        if (activeSkills == null || activeSkills.isEmpty()) {
            return "";
        }
        return renderSkillSystem(List.of(), activeSkills, null);
    }

    public static String renderSkillSystem(List<Skill> availableSkills, List<Skill> activeSkills, String skillsContainerPath) {
        if ((availableSkills == null || availableSkills.isEmpty()) && (activeSkills == null || activeSkills.isEmpty())) {
            return "";
        }
        String containerPath = skillsContainerPath != null ? skillsContainerPath : "/mnt/skills";
        StringBuilder sb = new StringBuilder();
        sb.append("<skill_system>\n");
        sb.append("You have access to skills that provide optimized workflows for specific tasks. ");
        sb.append("Each skill contains best practices, frameworks, and references to additional resources.\n\n");

        sb.append("**Progressive Loading Pattern:**\n");
        sb.append("1. When a user query matches a skill's use case, immediately call `read_file` on the skill's main file using the path attribute provided in the skill tag below\n");
        sb.append("2. Read and understand the skill's workflow and instructions\n");
        sb.append("3. The skill file contains references to external resources under the same folder\n");
        sb.append("4. Load referenced resources only when needed during execution\n");
        sb.append("5. Follow the skill's instructions precisely\n\n");

        sb.append("**Explicit Slash Skill Activation:**\n");
        sb.append("- If the user starts a request with `/<skill-name>`, that skill was explicitly requested for the current turn.\n");
        sb.append("- Follow the activated skill before choosing a general workflow.\n");
        sb.append("- The runtime injects the activated skill content for explicit slash activations; do not call `read_file` for that SKILL.md again unless the injected skill references supporting resources you need.\n\n");

        sb.append("Skills are located at: ").append(containerPath).append("\n\n");

        if (availableSkills != null && !availableSkills.isEmpty()) {
            List<Skill> sortedAvailable = new java.util.ArrayList<>(availableSkills);
            sortedAvailable.sort(java.util.Comparator.comparing((Skill s) -> (s.source() == null ? "" : s.source()) + ":" + (s.name() == null ? "" : s.name())));
            sb.append("<available_skills>\n");
            for (Skill skill : sortedAvailable) {
                String sourceLabel = "custom".equalsIgnoreCase(skill.source()) ? "[custom, editable]" : "[built-in]";
                String location = containerPath + "/" + skill.source() + "/" + skill.name() + "/SKILL.md";
                sb.append("    <skill>\n");
                sb.append("        <name>").append(skill.name()).append("</name>\n");
                sb.append("        <description>").append(skill.description()).append(" ").append(sourceLabel).append("</description>\n");
                sb.append("        <source>").append(skill.source()).append("</source>\n");
                sb.append("        <location>").append(location).append("</location>\n");
                sb.append("    </skill>\n");
            }
            sb.append("</available_skills>\n\n");
        }

        if (activeSkills != null && !activeSkills.isEmpty()) {
            List<Skill> sortedActive = new java.util.ArrayList<>(activeSkills);
            sortedActive.sort(java.util.Comparator.comparing((Skill s) -> (s.source() == null ? "" : s.source()) + ":" + (s.name() == null ? "" : s.name())));
            sb.append("<active_skills>\n");
            for (Skill skill : sortedActive) {
                sb.append("    <activated_skill name=\"").append(skill.name()).append("\">\n");
                sb.append("====== SKILL CONTENT START: ").append(skill.name()).append(" ======\n");
                sb.append(skill.skillMdContent()).append("\n");
                sb.append("====== SKILL CONTENT END: ").append(skill.name()).append(" ======\n");
                sb.append("    </activated_skill>\n");
            }
            sb.append("</active_skills>\n");
        }

        sb.append("</skill_system>");
        return sb.toString();
    }

    public static String injectIntoSystemPrompt(String systemPrompt, String skillsSection) {
        if (skillsSection == null || skillsSection.isBlank()) {
            return systemPrompt == null ? "" : systemPrompt;
        }
        String base = systemPrompt == null ? "" : systemPrompt;
        if (base.contains("{skills_section}")) {
            return base.replace("{skills_section}", skillsSection);
        }
        if (base.isBlank()) {
            return skillsSection.trim();
        }
        return base + "\n\n" + skillsSection.trim();
    }
}
