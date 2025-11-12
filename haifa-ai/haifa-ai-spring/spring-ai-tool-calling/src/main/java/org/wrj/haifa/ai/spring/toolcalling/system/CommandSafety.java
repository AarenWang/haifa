package org.wrj.haifa.ai.spring.toolcalling.system;

import java.util.List;
import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * Utility methods that verify whether a command suggested by the LLM is considered
 * safe to run on the local machine.
 */
public final class CommandSafety {

    private static final List<String> FORBIDDEN_SUBSTRINGS = List.of(
            " rm ",
            " rm-",
            " rmdir",
            " mkfs",
            " shutdown",
            " reboot",
            " halt",
            " poweroff",
            " init 0",
            " init 6",
            " kill ",
            " pkill",
            " :> ",
            " >/dev",
            " chmod ",
            " chown "
    );

    private CommandSafety() {
    }

    public static boolean isSafe(String command) {
        if (!StringUtils.hasText(command)) {
            return false;
        }
        String normalized = (" " + command + " ").toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_SUBSTRINGS) {
            if (normalized.contains(forbidden)) {
                return false;
            }
        }
        return true;
    }
}
