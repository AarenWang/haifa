package org.wrj.haifa.ai.spring.toolcalling.system;

/**
 * Result of executing a local system command.
 */
public record CommandResult(String description, String command, String stdout, String stderr, int exitCode) {

    public CommandResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
