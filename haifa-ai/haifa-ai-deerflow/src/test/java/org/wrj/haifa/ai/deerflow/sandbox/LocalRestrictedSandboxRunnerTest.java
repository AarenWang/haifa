package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRestrictedSandboxRunnerTest {

    @Test
    void runsCommandInsideSandboxWorkdir(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);

        SandboxResult result = runner.run(new SandboxRequest(
                currentDirectoryCommand(),
                tmp.resolve("workspace"),
                null,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-1"
        ));

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.metadata()).containsEntry("sandboxBackend", "local");
        assertThat(result.metadata()).containsEntry("strongIsolation", false);
        assertThat(result.metadata()).containsEntry("workspaceMounted", false);
        assertThat(result.metadata()).containsKey("isolationWarning");
        Path workdir = Path.of((String) result.metadata().get("sandboxWorkdir"));
        assertThat(workdir).startsWith(tmp.resolve("outputs").resolve("sandbox").resolve("run-1"));
        assertThat(Files.isDirectory(workdir)).isTrue();
    }

    @Test
    void ignoresCallerProvidedWorkdirAndCreatesUniqueSandboxWorkdir(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);
        Path requested = tmp.resolve("caller-workdir");

        SandboxResult first = runner.run(new SandboxRequest(
                currentDirectoryCommand(),
                tmp.resolve("workspace"),
                requested,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-unique"
        ));
        SandboxResult second = runner.run(new SandboxRequest(
                currentDirectoryCommand(),
                tmp.resolve("workspace"),
                requested,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-unique"
        ));

        Path firstWorkdir = Path.of((String) first.metadata().get("sandboxWorkdir"));
        Path secondWorkdir = Path.of((String) second.metadata().get("sandboxWorkdir"));
        assertThat(firstWorkdir).isNotEqualTo(requested.toAbsolutePath().normalize());
        assertThat(secondWorkdir).isNotEqualTo(requested.toAbsolutePath().normalize());
        assertThat(firstWorkdir).isNotEqualTo(secondWorkdir);
    }

    @Test
    void terminatesTimedOutCommand(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);

        SandboxResult result = runner.run(new SandboxRequest(
                slowCommand(),
                tmp.resolve("workspace"),
                null,
                Duration.ofMillis(100),
                Map.of(),
                false,
                "run-timeout"
        ));

        assertThat(result.timedOut()).isTrue();
        assertThat(result.exitCode()).isEqualTo(-1);
    }

    @Test
    void truncatesLargeOutput(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        properties.getSandbox().setMaxOutputChars(1200);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);

        SandboxResult result = runner.run(new SandboxRequest(
                largeOutputCommand(),
                tmp.resolve("workspace"),
                null,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-output"
        ));

        assertThat(result.outputTruncated()).isTrue();
        assertThat(result.stdout().length() + result.stderr().length()).isLessThanOrEqualTo(1300);
    }

    private static DeerFlowProperties properties(Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(tmp.resolve("outputs").toString());
        properties.getSandbox().setWorkdirSubdir("sandbox");
        return properties;
    }

    private static String slowCommand() {
        return isWindows() ? "ping -n 3 127.0.0.1 > nul" : "sleep 2";
    }

    private static String currentDirectoryCommand() {
        return isWindows() ? "cd" : "pwd";
    }

    private static String largeOutputCommand() {
        if (isWindows()) {
            return "for /L %i in (1,1,300) do @echo 0123456789";
        }
        return "yes 0123456789 | head -n 300";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
