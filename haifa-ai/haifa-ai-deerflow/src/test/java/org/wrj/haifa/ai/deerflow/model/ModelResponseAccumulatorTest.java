package org.wrj.haifa.ai.deerflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.model.cache.ModelUsage;
import org.wrj.haifa.ai.deerflow.model.cache.UsageAvailability;

class ModelResponseAccumulatorTest {

    @Test
    void mergesFragmentedToolCallByIdAndOrdinal() {
        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();

        accumulator.accumulate(response(new ModelToolCall("call-1", "get_", "{\"city\":", "tool_call")));
        accumulator.accumulate(response(new ModelToolCall("call-1", "weather", "\"Paris\"}", "tool_call")));

        assertThat(accumulator.toResponse().toolCalls())
                .containsExactly(new ModelToolCall(
                        "call-1", "get_weather", "{\"city\":\"Paris\"}", "tool_call"));
    }

    @Test
    void deduplicatesCumulativeStreamingChunks() {
        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();

        accumulator.accumulate(response(new ModelToolCall("call-1", "weather", "{\"city\":", "tool_call")));
        accumulator.accumulate(response(new ModelToolCall(
                "call-1", "weather", "{\"city\":\"Paris\"}", "tool_call")));
        accumulator.accumulate(response(new ModelToolCall(
                "call-1", "weather", "{\"city\":\"Paris\"}", "tool_call")));

        assertThat(accumulator.toResponse().toolCalls())
                .singleElement()
                .extracting(ModelToolCall::arguments)
                .isEqualTo("{\"city\":\"Paris\"}");
    }

    @Test
    void keepsParallelCallsInTheirOriginalOrder() {
        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();
        accumulator.accumulate(new ModelResponse("", List.of(
                new ModelToolCall("call-1", "first", "{}"),
                new ModelToolCall("call-2", "second", "{}")),
                List.of(), null, Map.of(), ModelProtocolState.empty()));

        assertThat(accumulator.toResponse().toolCalls())
                .extracting(ModelToolCall::id)
                .containsExactly("call-1", "call-2");
    }

    @Test
    void rejectsConflictingStableFields() {
        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();
        accumulator.accumulate(response(new ModelToolCall("call-1", "weather", "{}", "tool_call")));

        assertThatThrownBy(() -> accumulator.accumulate(
                response(new ModelToolCall("call-1", "weather", "{}", "other_type"))))
                .isInstanceOf(ModelProtocolStateException.class)
                .hasMessageContaining("type");
    }

    @Test
    void keepsLatestStreamingUsageSnapshotInsteadOfSummingSnapshots() {
        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();
        accumulator.accumulate(new ModelResponse("a", List.of(), List.of(), null, Map.of(),
                ModelProtocolState.empty(), usage(10L, 2L)));
        accumulator.accumulate(new ModelResponse("b", List.of(), List.of(), null, Map.of(),
                ModelProtocolState.empty(), usage(12L, 4L)));

        assertThat(accumulator.toResponse().usage().inputTokens()).isEqualTo(12L);
        assertThat(accumulator.toResponse().usage().cacheReadInputTokens()).isEqualTo(4L);
    }

    private static ModelUsage usage(Long input, Long cacheRead) {
        return new ModelUsage(input, input - cacheRead, 3L, input + 3L, cacheRead, null, null,
                "test", "model", UsageAvailability.PROVIDER_REPORTED, Map.of());
    }

    private static ModelResponse response(ModelToolCall call) {
        return new ModelResponse("", List.of(call), List.of(), null, Map.of(), ModelProtocolState.empty());
    }
}
