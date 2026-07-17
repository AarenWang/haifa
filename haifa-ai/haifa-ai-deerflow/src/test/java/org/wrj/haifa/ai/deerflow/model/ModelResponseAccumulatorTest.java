package org.wrj.haifa.ai.deerflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

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

    private static ModelResponse response(ModelToolCall call) {
        return new ModelResponse("", List.of(call), List.of(), null, Map.of(), ModelProtocolState.empty());
    }
}
