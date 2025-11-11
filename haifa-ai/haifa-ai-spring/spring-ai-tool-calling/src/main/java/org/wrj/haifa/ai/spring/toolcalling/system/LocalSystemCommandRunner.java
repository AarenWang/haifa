package org.wrj.haifa.ai.spring.toolcalling.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation that executes commands on the underlying operating
 * system using {@link ProcessBuilder}.
 */
@Component
public class LocalSystemCommandRunner implements SystemCommandRunner {

    private static final Logger logger = LoggerFactory.getLogger(LocalSystemCommandRunner.class);

    @Override
    public CommandResult run(String description, List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);
        try {
            Process process = builder.start();
            String stdout = readFully(process.getInputStream());
            String stderr = readFully(process.getErrorStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Command '{}' failed with exit code {}: {}", command, exitCode, stderr);
            }
            return new CommandResult(description, String.join(" ", command), stdout, stderr, exitCode);
        }
        catch (IOException ex) {
            logger.error("Command '{}' failed: {}", command, ex.getMessage());
            return new CommandResult(description, String.join(" ", command), "", ex.getMessage(), -1);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("Command '{}' interrupted: {}", command, ex.getMessage());
            return new CommandResult(description, String.join(" ", command), "", ex.getMessage(), -1);
        }
    }

    private String readFully(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
