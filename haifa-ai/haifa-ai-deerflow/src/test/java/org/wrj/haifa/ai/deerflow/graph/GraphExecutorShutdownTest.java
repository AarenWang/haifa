package org.wrj.haifa.ai.deerflow.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;

class GraphExecutorShutdownTest {

    @Test
    void allPoolsAreManagedAndConfigurationIsValidated() throws Exception {
        GraphExecutionManager manager = GraphExecutorIsolationTest.manager(1, 1, 2, 1, 1, 2, 1, 1, 2);
        manager.destroy();
        assertThat(manager.coordinatorStatus().shutdown()).isTrue();
        assertThat(manager.modelStatus().shutdown()).isTrue();
        assertThat(manager.toolStatus().shutdown()).isTrue();

        GraphExecutorProperties invalid = new GraphExecutorProperties();
        invalid.setCoordinatorCorePoolSize(2);
        invalid.setCoordinatorMaxPoolSize(1);
        assertThatThrownBy(() -> new GraphExecutionManager(invalid).afterPropertiesSet())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coordinator");
    }
}
