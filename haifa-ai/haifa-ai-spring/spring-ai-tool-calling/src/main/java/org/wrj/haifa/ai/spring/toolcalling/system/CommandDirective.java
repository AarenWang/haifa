package org.wrj.haifa.ai.spring.toolcalling.system;

import java.util.List;

/**
 * Describes the information gathering goal for a system command. It is provided to
 * the planner so it can decide which shell command should be executed.
 */
public record CommandDirective(String identifier, String goal, String defaultCommand, List<String> allowedExamples) {

    public CommandDirective {
        allowedExamples = allowedExamples == null ? List.of() : List.copyOf(allowedExamples);
    }
}
