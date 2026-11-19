package org.wrj.haifa.ai.deerflow.middleware;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver.FilteredToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.subagent.SubagentRuntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubagentLimitMiddlewareTest {

    private DeerFlowProperties properties;
    private SubagentRuntime subagentRuntime;
    private SubagentLimitMiddleware middleware;
    private AgentRunConfig runConfig;

    @BeforeEach
    void setUp() {
        properties = mock(DeerFlowProperties.class);
        subagentRuntime = mock(SubagentRuntime.class);

        when(properties.getSubagentMaxPerResponse()).thenReturn(2);
        when(properties.getSubagentMaxConcurrent()).thenReturn(3);

        middleware = new SubagentLimitMiddleware(properties, subagentRuntime);
        runConfig = new AgentRunConfig("thread-1", "run-1", "test-model", false, false,
                10, Path.of("."), RunMode.RESEARCH, null, Map.of());
    }

    @Test
    void allowsNonTaskToolsAlways() {
        ToolCall tc1 = ToolCall.of("call-1", "web_search", "{}");
        ToolCall tc2 = ToolCall.of("call-2", "web_fetch", "{}");

        List<FilteredToolCall> filtered = middleware.afterToolCallsParsed(runConfig, List.of(tc1, tc2));

        assertThat(filtered).hasSize(2);
        assertThat(filtered.get(0).allowed()).isTrue();
        assertThat(filtered.get(1).allowed()).isTrue();
    }

    @Test
    void allowsTaskToolsWithinLimits() {
        when(subagentRuntime.activeCount("run-1")).thenReturn(0);

        ToolCall tc1 = ToolCall.of("call-1", "task", "{}");
        ToolCall tc2 = ToolCall.of("call-2", "task", "{}");

        List<FilteredToolCall> filtered = middleware.afterToolCallsParsed(runConfig, List.of(tc1, tc2));

        assertThat(filtered).hasSize(2);
        assertThat(filtered.get(0).allowed()).isTrue();
        assertThat(filtered.get(1).allowed()).isTrue();
    }

    @Test
    void rejectsTaskCallsExceedingPerResponseLimit() {
        when(subagentRuntime.activeCount("run-1")).thenReturn(0);

        ToolCall tc1 = ToolCall.of("call-1", "task", "{}");
        ToolCall tc2 = ToolCall.of("call-2", "task", "{}");
        ToolCall tc3 = ToolCall.of("call-3", "task", "{}");

        List<FilteredToolCall> filtered = middleware.afterToolCallsParsed(runConfig, List.of(tc1, tc2, tc3));

        assertThat(filtered).hasSize(3);
        assertThat(filtered.get(0).allowed()).isTrue();
        assertThat(filtered.get(1).allowed()).isTrue();
        assertThat(filtered.get(2).allowed()).isFalse();
        assertThat(filtered.get(2).reason()).contains("max 2 task calls per response");

        assertThat(middleware.getRejectionCount("run-1")).isEqualTo(1);
    }

    @Test
    void rejectsTaskCallsExceedingConcurrentLimit() {
        // 2 currently running, max is 3. We try to launch 2 more.
        when(subagentRuntime.activeCount("run-1")).thenReturn(2);

        ToolCall tc1 = ToolCall.of("call-1", "task", "{}");
        ToolCall tc2 = ToolCall.of("call-2", "task", "{}");

        List<FilteredToolCall> filtered = middleware.afterToolCallsParsed(runConfig, List.of(tc1, tc2));

        assertThat(filtered).hasSize(2);
        // The first task call brings active + count to 3, which is allowed.
        assertThat(filtered.get(0).allowed()).isTrue();
        // The second task call would bring it to 4, which exceeds the limit of 3, so it is rejected.
        assertThat(filtered.get(1).allowed()).isFalse();
        assertThat(filtered.get(1).reason()).contains("max 3 concurrent subagents per run");

        assertThat(middleware.getRejectionCount("run-1")).isEqualTo(1);
    }

    @Test
    void rejectsTaskCallsAfterProviderConfigurationFailure() {
        when(subagentRuntime.hasProviderConfigurationFailure("run-1")).thenReturn(true);
        when(subagentRuntime.providerConfigurationFailureReason("run-1"))
                .thenReturn("Subagent dispatch circuit is open because an earlier provider request returned HTTP 400.");

        List<FilteredToolCall> filtered = middleware.afterToolCallsParsed(
                runConfig, List.of(ToolCall.of("call-1", "task", "{}")));

        assertThat(filtered).singleElement().satisfies(call -> {
            assertThat(call.allowed()).isFalse();
            assertThat(call.reason()).contains("circuit is open");
        });
        assertThat(middleware.getRejectionCount("run-1")).isEqualTo(1);
    }
}
