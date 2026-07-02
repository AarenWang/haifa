package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerSandboxRunnerTest {

    @Test
    void rejectsOutOfBoundsCallerProvidedWorkdir(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(tmp.resolve("outputs").toString());
        properties.getSandbox().setWorkdirSubdir("sandbox");

        DockerSandboxRunner runner = new DockerSandboxRunner(properties);
        Path outOfBounds = tmp.resolve("out-of-bounds");

        assertThatThrownBy(() -> {
            runner.run(new SandboxRequest(
                    "ls",
                    tmp.resolve("workspace"),
                    outOfBounds,
                    Duration.ofSeconds(5),
                    Map.of(),
                    false,
                    "run-unique"
            ));
        }).isInstanceOf(SecurityException.class).hasMessageContaining("outside of allowed sandbox root");
    }
}
