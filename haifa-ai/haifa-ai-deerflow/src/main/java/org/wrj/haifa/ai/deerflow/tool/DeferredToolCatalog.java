package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.skill.SkillStorage;

@Component
public class DeferredToolCatalog {

    private List<AgentTool> builtinTools;
    private List<Skill> allSkills;
    private ToolRegistry toolRegistry;
    private SkillStorage skillStorage;

    public DeferredToolCatalog(@Lazy ToolRegistry toolRegistry, SkillStorage skillStorage) {
        this.toolRegistry = toolRegistry;
        this.skillStorage = skillStorage;
    }

    public DeferredToolCatalog(List<AgentTool> builtinTools, List<Skill> allSkills) {
        this.builtinTools = List.copyOf(builtinTools);
        this.allSkills = List.copyOf(allSkills);
    }

    private List<AgentTool> builtinTools() {
        if (builtinTools == null) {
            builtinTools = toolRegistry == null ? List.of() : toolRegistry.tools();
        }
        return builtinTools;
    }

    private List<Skill> allSkills() {
        if (allSkills == null) {
            allSkills = skillStorage == null ? List.of() : skillStorage.listAll();
        }
        return allSkills;
    }

    public List<ToolDescriptor> search(String keyword) {
        String lower = keyword == null ? "" : keyword.toLowerCase();
        List<ToolDescriptor> results = new java.util.ArrayList<>();
        for (AgentTool tool : builtinTools()) {
            if (matches(tool.name(), tool.description(), lower)) {
                results.add(new ToolDescriptor(tool.name(), tool.description(), "builtin", false));
            }
        }
        for (Skill skill : allSkills()) {
            if (skill.allowedTools() != null) {
                for (String toolName : skill.allowedTools()) {
                    if (toolName.toLowerCase().contains(lower) || skill.name().toLowerCase().contains(lower)) {
                        results.add(new ToolDescriptor(
                                toolName,
                                "Provided by skill: " + skill.name() + " (" + skill.description() + ")",
                                "skill:" + skill.name(),
                                true
                        ));
                    }
                }
            }
        }
        return results;
    }

    public List<ToolDescriptor> listAll() {
        List<ToolDescriptor> results = new java.util.ArrayList<>();
        for (AgentTool tool : builtinTools()) {
            results.add(new ToolDescriptor(tool.name(), tool.description(), "builtin", false));
        }
        for (Skill skill : allSkills()) {
            if (skill.allowedTools() != null) {
                for (String toolName : skill.allowedTools()) {
                    results.add(new ToolDescriptor(
                            toolName,
                            "Provided by skill: " + skill.name(),
                            "skill:" + skill.name(),
                            true
                    ));
                }
            }
        }
        return results;
    }

    private static boolean matches(String name, String description, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        String n = name == null ? "" : name.toLowerCase();
        String d = description == null ? "" : description.toLowerCase();
        return n.contains(keyword) || d.contains(keyword);
    }
}
