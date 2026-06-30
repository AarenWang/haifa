package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.skill.Skill;

@Component
public class ToolPolicyService {

    private final Set<String> builtinToolNames;

    public ToolPolicyService(List<AgentTool> builtinTools) {
        this.builtinToolNames = builtinTools.stream()
                .map(AgentTool::name)
                .collect(java.util.HashSet::new, java.util.HashSet::add, java.util.HashSet::addAll);
    }

    public boolean isToolAllowed(String toolName, List<Skill> activeSkills) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }
        // Built-in tools are always allowed
        if (builtinToolNames.contains(toolName)) {
            return true;
        }
        // No active skills => no extra permissions, unknown tool is disallowed
        if (activeSkills == null || activeSkills.isEmpty()) {
            return false;
        }
        // Check if any active skill explicitly allows this tool
        for (Skill skill : activeSkills) {
            Set<String> allowed = skill.allowedTools();
            if (allowed != null && allowed.contains(toolName)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> allowedToolsForSkills(List<Skill> activeSkills) {
        Set<String> allowed = new java.util.HashSet<>(builtinToolNames);
        if (activeSkills != null) {
            for (Skill skill : activeSkills) {
                if (skill.allowedTools() != null) {
                    allowed.addAll(skill.allowedTools());
                }
            }
        }
        return Set.copyOf(allowed);
    }
}
