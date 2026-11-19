package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolCallEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.ToolCallMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.ToolCallRepository;

@SpringBootTest
@ActiveProfiles("test")
class ToolCallStoreTest {

    @Autowired
    private ToolCallStore toolCallStore;

    @Autowired
    private ToolCallRepository toolCallRepository;

    @Autowired
    private ToolCallMapper toolCallMapper;

    @Test
    void resumesSequenceFromPersistedRows() {
        ToolCallEntity first = toolCallStore.saveRequested(ToolCall.of("search", "{\"q\":\"one\"}"), "run-seq", "thread-seq");

        ToolCallStore reloadedStore = new ToolCallStore(toolCallRepository, toolCallMapper);
        ToolCallEntity second = reloadedStore.saveRequested(ToolCall.of("fetch", "{\"url\":\"two\"}"), "run-seq", "thread-seq");

        assertThat(second.getSequenceNo()).isEqualTo(first.getSequenceNo() + 1);
    }
}
