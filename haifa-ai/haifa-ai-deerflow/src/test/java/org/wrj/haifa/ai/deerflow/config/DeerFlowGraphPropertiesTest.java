package org.wrj.haifa.ai.deerflow.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DeerFlowGraphPropertiesTest {

    @Test
    void graphRuntimeDefaultsToDisabledOffModeWithoutCheckpoint() {
        DeerFlowProperties properties = new DeerFlowProperties();

        assertThat(properties.getGraph().isEnabled()).isFalse();
        assertThat(properties.getGraph().getMode()).isEqualTo(GraphRuntimeMode.OFF);
        assertThat(properties.getGraph().getCheckpoint().isEnabled()).isFalse();
    }

    @Test
    void nullGraphAssignmentsFallBackToSafeDefaults() {
        DeerFlowProperties properties = new DeerFlowProperties();

        properties.setGraph(null);
        properties.getGraph().setMode(null);
        properties.getGraph().setCheckpoint(null);

        assertThat(properties.getGraph().isEnabled()).isFalse();
        assertThat(properties.getGraph().getMode()).isEqualTo(GraphRuntimeMode.OFF);
        assertThat(properties.getGraph().getCheckpoint().isEnabled()).isFalse();
    }

    @Test
    void defaultOutputsRootFollowsDiscoveredRepositoryRoot(@TempDir Path tempDir) throws Exception {
        Path repo = tempDir.resolve("haifa");
        Path module = repo.resolve("haifa-ai").resolve("haifa-ai-deerflow");
        Files.createDirectories(module);
        Files.writeString(repo.resolve("AGENTS.md"), "agent guide");
        Files.writeString(repo.resolve("pom.xml"), "<project />");
        Files.writeString(module.resolve("pom.xml"), "<project />");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", module.toString());
            DeerFlowProperties properties = new DeerFlowProperties();
            properties.setWorkspaceRoot(module.toString());

            assertThat(Path.of(properties.getWorkspaceRoot())).isEqualTo(repo.toAbsolutePath().normalize());
            assertThat(Path.of(properties.getOutputsRoot())).isEqualTo(repo.resolve("outputs").toAbsolutePath().normalize());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}
