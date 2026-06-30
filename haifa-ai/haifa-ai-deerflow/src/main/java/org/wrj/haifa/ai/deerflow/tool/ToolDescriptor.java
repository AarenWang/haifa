package org.wrj.haifa.ai.deerflow.tool;

public record ToolDescriptor(
        String name,
        String description,
        String source,
        boolean requiresSkillActivation
) {
}
