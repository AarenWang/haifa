package org.wrj.haifa.ai.deerflow.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisabledMcpClientAdapterTest {

    @Test
    void isEnabledReturnsFalse() {
        DisabledMcpClientAdapter adapter = new DisabledMcpClientAdapter();
        assertThat(adapter.isEnabled()).isFalse();
    }

    @Test
    void listToolsReturnsEmpty() {
        DisabledMcpClientAdapter adapter = new DisabledMcpClientAdapter();
        assertThat(adapter.listTools()).isEmpty();
    }

    @Test
    void executeToolThrows() {
        DisabledMcpClientAdapter adapter = new DisabledMcpClientAdapter();
        assertThatThrownBy(() -> adapter.executeTool("anything", "{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MCP is disabled");
    }
}
