package org.wrj.haifa.ai.deerflow.subagent;

import java.util.List;

/**
 * Configuration for a subagent.
 *
 * <p>Heavily inspired by DeerFlow Python {@code SubagentConfig}.
 */
public record SubagentConfig(
        String name,
        String description,
        String systemPrompt,
        List<String> allowedTools,
        List<String> disallowedTools,
        String model,
        int maxTurns,
        long timeoutSeconds
) {

    public SubagentConfig {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
        disallowedTools = disallowedTools == null ? List.of("task") : List.copyOf(disallowedTools);
        model = model == null ? "inherit" : model;
        maxTurns = Math.max(1, Math.min(100, maxTurns));
        timeoutSeconds = Math.max(10, Math.min(3600, timeoutSeconds));
    }

    public static SubagentConfig generalPurpose() {
        return new SubagentConfig(
                "general-purpose",
                "For ANY non-trivial task - web research, code exploration, file operations, analysis, etc.",
                "You are a capable subagent executing a focused sub-task. Be thorough but concise. "
                        + "Always cite your sources. Return a structured summary of your findings.",
                null,
                List.of("task"),
                "inherit",
                50,
                900
        );
    }

    public static SubagentConfig bash() {
        return new SubagentConfig(
                "bash",
                "Command execution specialist for running bash commands.",
                "You are a command execution subagent. Run the requested bash commands safely. "
                        + "Return command output and any errors encountered.",
                List.of("bash", "ls", "read_file", "glob", "grep", "write_file", "str_replace"),
                List.of("task", "web_search", "web_fetch"),
                "inherit",
                30,
                600
        );
    }

    public SubagentConfig withModel(String model) {
        return new SubagentConfig(name, description, systemPrompt, allowedTools, disallowedTools, model, maxTurns, timeoutSeconds);
    }

    public SubagentConfig withMaxTurns(int maxTurns) {
        return new SubagentConfig(name, description, systemPrompt, allowedTools, disallowedTools, model, maxTurns, timeoutSeconds);
    }

    public SubagentConfig withTimeoutSeconds(long timeoutSeconds) {
        return new SubagentConfig(name, description, systemPrompt, allowedTools, disallowedTools, model, maxTurns, timeoutSeconds);
    }
}
