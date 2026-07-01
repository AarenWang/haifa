package org.wrj.haifa.ai.deerflow.config;

import org.junit.jupiter.api.Test;

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
}
