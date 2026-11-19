package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedger;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedgerStore;

@SpringBootTest
@ActiveProfiles("test")
class BudgetLedgerTest {

    @Autowired
    private BudgetLedgerStore budgetLedgerStore;

    @Test
    void testBudgetTracking() {
        String runId = "run-budget-1";

        BudgetLedger ledger = budgetLedgerStore.init(runId, 600000L, 10, 30, 15, 10, 3, 2);
        assertThat(ledger.getRunId()).isEqualTo(runId);
        assertThat(ledger.getUsedElapsedMs()).isEqualTo(0L);

        budgetLedgerStore.incrementModelCalls(runId);
        budgetLedgerStore.incrementToolCalls(runId);
        budgetLedgerStore.incrementSearchQueries(runId);
        budgetLedgerStore.incrementFetchedSources(runId);
        budgetLedgerStore.incrementReplans(runId);
        budgetLedgerStore.incrementSubagents(runId);
        budgetLedgerStore.updateElapsed(runId, 5000L);
        budgetLedgerStore.updateStopReason(runId, "MAX_SEARCH_QUERIES_REACHED");

        BudgetLedger updated = budgetLedgerStore.findByRunId(runId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getUsedModelCalls()).isEqualTo(1);
        assertThat(updated.getUsedToolCalls()).isEqualTo(1);
        assertThat(updated.getUsedSearchQueries()).isEqualTo(1);
        assertThat(updated.getUsedFetchedSources()).isEqualTo(1);
        assertThat(updated.getUsedReplans()).isEqualTo(1);
        assertThat(updated.getUsedSubagents()).isEqualTo(1);
        assertThat(updated.getUsedElapsedMs()).isEqualTo(5000L);
        assertThat(updated.getStopReason()).isEqualTo("MAX_SEARCH_QUERIES_REACHED");
    }
}
