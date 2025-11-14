package org.wrj.haifa.ai.spring.toolcalling.system;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Default planner that asks a large language model to generate a safe shell command
 * before the system information tool executes it locally.
 */
@Component
public class SafeSystemCommandPlanner implements SystemCommandPlanner {

    private static final Logger logger = LoggerFactory.getLogger(SafeSystemCommandPlanner.class);

    private static final String SYSTEM_PROMPT = "You help plan safe POSIX shell commands that only inspect the system. "
            + "Never suggest commands that delete, modify, shutdown or otherwise damage the host.";

    private final ChatClient chatClient;

    public SafeSystemCommandPlanner(ObjectProvider<ChatClient.Builder> builderProvider) {
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        this.chatClient = builder != null ? builder.build() : null;
    }

    @Override
    public PlannedCommand plan(String instruction, CommandDirective directive) {
        if (this.chatClient == null) {
            return new PlannedCommand(directive.defaultCommand(), "llm-unavailable");
        }

        String userPrompt = buildUserPrompt(instruction, directive);
        try {
            String response = this.chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            String command = extractCommand(response);
            if (!CommandSafety.isSafe(command)) {
                logger.warn("Discarding unsafe command '{}' suggested for {}", command, directive.identifier());
                command = directive.defaultCommand();
            }
            else if (!StringUtils.hasText(command)) {
                command = directive.defaultCommand();
            }
            return new PlannedCommand(command, response);
        }
        catch (Exception ex) {
            logger.warn("Failed to plan command for {}: {}", directive.identifier(), ex.getMessage());
            return new PlannedCommand(directive.defaultCommand(), ex.getMessage());
        }
    }

    private String buildUserPrompt(String instruction, CommandDirective directive) {
        String safeInstruction = StringUtils.hasText(instruction) ? instruction.trim()
                : "No additional user preference was provided.";
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("User request: " + safeInstruction);
        joiner.add("Goal: Provide a single shell command that inspects " + directive.goal() + ".");
        joiner.add("The command must be read-only and safe. Do not include explanations.");
        if (!directive.allowedExamples().isEmpty()) {
            joiner.add("Safe examples include: " + String.join(", ", directive.allowedExamples()) + ".");
        }
        joiner.add("Return only the command to execute. No backticks or prose.");
        return joiner.toString();
    }

    private String extractCommand(String response) {
        if (!StringUtils.hasText(response)) {
            return "";
        }
        String trimmed = response.trim();
        int codeFence = trimmed.indexOf("```");
        if (codeFence >= 0) {
            int endFence = trimmed.indexOf("```", codeFence + 3);
            if (endFence > codeFence) {
                trimmed = trimmed.substring(codeFence + 3, endFence).trim();
            }
        }
        int newline = trimmed.indexOf('\n');
        if (newline >= 0) {
            trimmed = trimmed.substring(0, newline).trim();
        }
        if (trimmed.toLowerCase().startsWith("command:")) {
            trimmed = trimmed.substring("command:".length()).trim();
        }
        if (trimmed.startsWith("`") && trimmed.endsWith("`")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
