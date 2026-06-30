package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AgentEventStoreTest {

    @Autowired
    private AgentEventStore agentEventStore;

    @Autowired
    private RunManager runManager;

    @Test
    void savesAndRetrievesEventsByRunId() {
        RunRecord run = runManager.create("thread-events", "model", Map.of());
        String runId = run.runId();

        AgentEvent event1 = AgentEvent.of("1", runId, "thread-events", AgentEventType.RUN_STARTED, "Started", Map.of());
        AgentEvent event2 = AgentEvent.of("2", runId, "thread-events", AgentEventType.RUN_COMPLETED, "Done", Map.of());

        agentEventStore.save(event1);
        agentEventStore.save(event2);

        var events = agentEventStore.findByRunId(runId);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).type()).isEqualTo(AgentEventType.RUN_STARTED);
        assertThat(events.get(1).type()).isEqualTo(AgentEventType.RUN_COMPLETED);
    }

    @Test
    void savesResearchEventTypes() {
        RunRecord run = runManager.create("thread-research", "model", Map.of("mode", "research"));
        String runId = run.runId();

        AgentEvent event = AgentEvent.of("1", runId, "thread-research", AgentEventType.RESEARCH_PLAN_CREATED,
                "Plan created", Map.of("depth", "STANDARD"));
        agentEventStore.save(event);

        var events = agentEventStore.findByRunId(runId);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).type()).isEqualTo(AgentEventType.RESEARCH_PLAN_CREATED);
    }

    @Test
    void findByThreadIdWorks() {
        RunRecord run = runManager.create("thread-query", "model", Map.of());
        String runId = run.runId();

        agentEventStore.save(AgentEvent.of("1", runId, "thread-query", AgentEventType.RUN_STARTED, "Start", Map.of()));

        var events = agentEventStore.findByThreadId("thread-query");
        assertThat(events).hasSize(1);
    }
}
