package org.wrj.haifa.ai.deerflow.config;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DeerFlowGraphPropertiesTest {

    @Test
    void graphRuntimeDefaultsToGraphFirstWithCheckpoint() {
        DeerFlowProperties properties = new DeerFlowProperties();

        assertThat(properties.getGraph().isEnabled()).isTrue();
        assertThat(properties.getGraph().getMode()).isEqualTo(GraphRuntimeMode.GRAPH_FIRST);
        assertThat(properties.getGraph().getCheckpoint().isEnabled()).isTrue();
    }

    @Test
    void nullGraphAssignmentsFallBackToSafeDefaults() {
        DeerFlowProperties properties = new DeerFlowProperties();

        properties.setGraph(null);
        properties.getGraph().setMode(null);
        properties.getGraph().setCheckpoint(null);

        assertThat(properties.getGraph().isEnabled()).isTrue();
        assertThat(properties.getGraph().getMode()).isEqualTo(GraphRuntimeMode.GRAPH_FIRST);
        assertThat(properties.getGraph().getCheckpoint().isEnabled()).isTrue();
    }

    @Test
    void defaultUserDataRootsUseStableSubdirectories(@TempDir Path tempDir) {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            DeerFlowProperties properties = new DeerFlowProperties();

            Path userData = tempDir.resolve("data/user-data").toAbsolutePath().normalize();
            assertThat(Path.of(properties.getUserDataRoot())).isEqualTo(userData);
            assertThat(Path.of(properties.getWorkspaceRoot())).isEqualTo(userData.resolve("workspace"));
            assertThat(Path.of(properties.getUploadsRoot())).isEqualTo(userData.resolve("uploads"));
            assertThat(Path.of(properties.getOutputsRoot())).isEqualTo(userData.resolve("outputs"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void explicitWorkspaceDoesNotMoveOutputsRoot(@TempDir Path tempDir) {
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            DeerFlowProperties properties = new DeerFlowProperties();
            Path explicitWorkspace = tempDir.resolve("custom-workspace");
            properties.setWorkspaceRoot(explicitWorkspace.toString());

            assertThat(Path.of(properties.getWorkspaceRoot())).isEqualTo(explicitWorkspace.toAbsolutePath().normalize());
            assertThat(Path.of(properties.getOutputsRoot()))
                    .isEqualTo(tempDir.resolve("data/user-data/outputs").toAbsolutePath().normalize());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}