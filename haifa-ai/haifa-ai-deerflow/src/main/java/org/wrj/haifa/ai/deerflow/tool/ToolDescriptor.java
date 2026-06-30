package org.wrj.haifa.ai.deerflow.tool;

/**
 * Descriptor for a tool available to the agent.
 *
 * @param name the stable tool name exposed to the model
 * @param description what the tool does
 * @param source builtin / configured / mcp / delegation
 * @param category UI grouping category
 * @param requiresSkillActivation true if this tool requires an active skill
 * @param providerInfo optional human-readable provider metadata (e.g. default provider, available providers)
 */
public record ToolDescriptor(
        String name,
        String description,
        String source,
        String category,
        boolean requiresSkillActivation,
        String providerInfo
) {

    public ToolDescriptor(String name, String description, String source, String category, boolean requiresSkillActivation) {
        this(name, description, source, category, requiresSkillActivation, null);
    }
}
