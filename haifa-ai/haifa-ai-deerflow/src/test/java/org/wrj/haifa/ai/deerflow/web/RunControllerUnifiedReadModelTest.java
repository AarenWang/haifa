package org.wrj.haifa.ai.deerflow.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.work.WorkItem;
import org.wrj.haifa.ai.deerflow.work.WorkItemStore;
import org.wrj.haifa.ai.deerflow.source.Source;
import org.wrj.haifa.ai.deerflow.source.SourceStore;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedger;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedgerStore;

@SpringBootTest
@ActiveProfiles("test")
class RunControllerUnifiedReadModelTest {

    @Autowired
    private RunController runController;

    @Autowired
    private WorkItemStore workItemStore;

    @Autowired
    private SourceStore sourceStore;

    @Autowired
    private BudgetLedgerStore budgetLedgerStore;

    @Test
    void testUnifiedReadEndpoints() {
        String runId = "unified-run-1";
        String threadId = "unified-thread-1";

        // Insert mock data
        workItemStore.create(runId, threadId, null, "research_question", "Test Q", "Test G", "high", "agent");
        sourceStore.discover(runId, threadId, "https://example.com/api", "Example API", "example.com");
        budgetLedgerStore.init(runId, 1000L, 5, 5, 5, 5, 2, 2);

        // Call controller endpoints
        List<WorkItem> workItems = runController.workItems(runId).block();
        assertThat(workItems).hasSize(1);
        assertThat(workItems.get(0).getTitle()).isEqualTo("Test Q");

        List<Source> sources = runController.sources(runId).block();
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getUrl()).isEqualTo("https://example.com/api");

        BudgetLedger budget = runController.budget(runId).block();
        assertThat(budget).isNotNull();
        assertThat(budget.getMaxModelCalls()).isEqualTo(5);
    }
}
