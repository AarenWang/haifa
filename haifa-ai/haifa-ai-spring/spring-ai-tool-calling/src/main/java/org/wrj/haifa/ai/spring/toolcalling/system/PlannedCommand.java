package org.wrj.haifa.ai.spring.toolcalling.system;

import org.springframework.util.StringUtils;

/**
 * Result produced by the command planner. It contains the command suggested by the
 * language model and the raw response for auditing.
 */
public record PlannedCommand(String command, String rationale) {

    public boolean hasCommand() {
        return StringUtils.hasText(this.command);
    }
}
