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
        assertThat(result.metadata()).containsEntry("sandboxBackend", "local-restricted");
        assertThat(result.metadata()).containsEntry("strongIsolation", false);
        assertThat(result.metadata()).containsEntry("workspaceMounted", false);
        assertThat(result.metadata()).containsKey("isolationWarning");
        Path workdir = Path.of((String) result.metadata().get("sandboxWorkdir"));
        assertThat(workdir).startsWith(tmp.resolve("workspace").resolve("sandbox").resolve("run-1"));
        assertThat(Files.isDirectory(workdir)).isTrue();
    }

    @Test
    void usesCallerProvidedWorkdirWhenValid(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);
        Path sandboxRoot = tmp.resolve("workspace").resolve("sandbox");
        Path requested = sandboxRoot.resolve("caller-workdir-1");

        SandboxResult result = runner.run(new SandboxRequest(
                currentDirectoryCommand(),
                tmp.resolve("workspace"),
                requested,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-unique"
        ));

        Path actualWorkdir = Path.of((String) result.metadata().get("sandboxWorkdir"));
        assertThat(actualWorkdir).isEqualTo(requested.toAbsolutePath().normalize());
        assertThat(Files.isDirectory(actualWorkdir)).isTrue();
    }

    @Test
    void rejectsOutOfBoundsCallerProvidedWorkdir(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);
        Path outOfBounds = tmp.resolve("out-of-bounds");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            runner.run(new SandboxRequest(
                    currentDirectoryCommand(),
                    tmp.resolve("workspace"),
                    outOfBounds,
                    Duration.ofSeconds(5),
                    Map.of(),
                    false,
                    "run-unique"
            ));
        }).isInstanceOf(SecurityException.class).hasMessageContaining("outside of allowed sandbox root");
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

    @Test
    void resolvesPathsAndMaintainsEnvironment(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalRestrictedSandboxRunner runner = new LocalRestrictedSandboxRunner(properties);

        // Verify that path resolution maps virtual paths in the command
        String echoCmd = "echo /mnt/user-data/workspace/test.txt";
        SandboxResult result = runner.run(new SandboxRequest(
                echoCmd,
                tmp.resolve("workspace"),
                null,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-paths"
        ));

        assertThat(result.exitCode()).isEqualTo(0);
        // Path should have been mapped to local workspace physically, and then masked back to virtual
        assertThat(result.stdout().trim()).contains("/mnt/user-data/workspace/test.txt");

        // Verify basic environment variable settings like PATH exist
        String envCmd = "printf '%s' \"$PATH\"";
        SandboxResult envResult = runner.run(new SandboxRequest(
                envCmd,
                tmp.resolve("workspace"),
                null,
                Duration.ofSeconds(5),
                Map.of(),
                false,
                "run-env"
        ));
        assertThat(envResult.exitCode()).isEqualTo(0);
    }

    private static DeerFlowProperties properties(Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(tmp.resolve("workspace").toString());
        properties.setOutputsRoot(tmp.resolve("outputs").toString());
        properties.getSandbox().setWorkdirSubdir("sandbox");
        return properties;
    }

    private static String slowCommand() {
        return "sleep 2";
    }

    private static String currentDirectoryCommand() {
        return "pwd";
    }

    private static String largeOutputCommand() {
        return "for i in {1..300}; do echo 0123456789; done";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
