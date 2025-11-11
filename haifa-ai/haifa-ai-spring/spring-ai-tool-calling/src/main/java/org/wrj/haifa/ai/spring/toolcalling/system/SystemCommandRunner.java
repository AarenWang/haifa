package org.wrj.haifa.ai.spring.toolcalling.system;

import java.util.List;

/**
 * Abstraction over local command execution to keep the {@code SystemInfoService}
 * testable.
 */
public interface SystemCommandRunner {

    CommandResult run(String description, List<String> command);
}
