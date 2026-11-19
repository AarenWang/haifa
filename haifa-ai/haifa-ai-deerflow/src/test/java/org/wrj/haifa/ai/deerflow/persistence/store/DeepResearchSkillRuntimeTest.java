package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedger;
import org.wrj.haifa.ai.deerflow.budget.BudgetLedgerStore;
import org.wrj.haifa.ai.deerflow.research.ResearchLoopObserver;
import org.wrj.haifa.ai.deerflow.source.Source;
import org.wrj.haifa.ai.deerflow.source.SourceStore;

@SpringBootTest
@ActiveProfiles("test")
class DeepResearchSkillRuntimeTest {

    @Autowired
    private BudgetLedgerStore budgetLedgerStore;

    @Autowired
    private SourceStore sourceStore;

    @Test
    void testBudgetEnforcement() {
        String runId = "test-runtime-run-1";
        // Init budget ledger with max search queries = 1
        budgetLedgerStore.init(runId, 60000L, 10, 10, 1, 10, 5, 5);

        ResearchLoopObserver observer = new ResearchLoopObserver(
                null, null, null, null, null, null,
                null, null, sourceStore, null, null, null,
                budgetLedgerStore, null, null
        );

        AgentRunConfig config = new AgentRunConfig(
                "thread-1", runId, "model", true, false,
                10, null, RunMode.RESEARCH, null, new HashMap<>()
        );

        // 1st search query execution
        ToolCall call1 = ToolCall.of("web_search", "{\"query\":\"test1\"}");
        observer.beforeToolExecute(config, call1);

        // 2nd search query execution -> returns a failed tool result so the run can finish with limitations.
        ToolCall call2 = ToolCall.of("web_search", "{\"query\":\"test2\"}");
        ToolCallResult result = observer.beforeToolExecute(config, call2);
        assertThat(result.status()).isEqualTo(ToolCallResult.Status.FAILED);
        assertThat(result.error()).contains("BUDGET_EXCEEDED");
    }

    @Test
    void testSourceIngestion() {
        String runId = "test-runtime-run-2";
        String threadId = "test-runtime-thread-2";

        ResearchLoopObserver observer = new ResearchLoopObserver(
                null, null, null, null, null, null,
                null, null, sourceStore, null, null, null,
                budgetLedgerStore, null, null
        );

        AgentRunConfig config = new AgentRunConfig(
                threadId, runId, "model", true, false,
                10, null, RunMode.RESEARCH, null, new HashMap<>()
        );

        ToolCall searchCall = ToolCall.of("web_search", "{\"query\":\"test\"}");
        ToolCallResult searchResult = new ToolCallResult(
                "call-1", "web_search", "{\"query\":\"test\"}",
                ToolCallResult.Status.SUCCESS,
                "[{\"title\":\"Example Page\",\"link\":\"https://example.com/page1\",\"snippet\":\"abc\"}]",
                "", 100L, Map.of()
        );

        observer.onToolCompleted(config, searchCall, searchResult, new ArrayList<>(), new AtomicInteger(), new ArrayList<>());

        List<Source> sources = sourceStore.findByRunId(runId);
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getUrl()).isEqualTo("https://example.com/page1");
        assertThat(sources.get(0).getTitle()).isEqualTo("Example Page");
    }
}
