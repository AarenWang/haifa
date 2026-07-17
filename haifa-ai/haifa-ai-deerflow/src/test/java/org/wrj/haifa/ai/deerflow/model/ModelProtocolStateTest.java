package org.wrj.haifa.ai.deerflow.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelProtocolStateTest {

    @Test
    void roundTripsJsonSafeState() {
        ModelProtocolState state = new ModelProtocolState(
                1, "google-genai", Map.of("thoughtSignatures", List.of("c2ln")), List.of());

        assertThat(ModelProtocolState.deserializeProtocolState(
                ModelProtocolState.serializeProtocolState(state))).isEqualTo(state);
    }

    @Test
    void rejectsUnknownSchemaVersion() {
        assertThatThrownBy(() -> ModelProtocolState.deserializeProtocolState(Map.of(
                "schemaVersion", 2,
                "adapter", "google-genai",
                "messageExtensions", Map.of(),
                "toolCallExtensions", List.of())))
                .isInstanceOf(ModelProtocolStateException.class)
                .hasMessageContaining("schema version");
    }

    @Test
    void rejectsMalformedPersistedStateInsteadOfDroppingIt() {
        assertThatThrownBy(() -> ModelProtocolState.deserializeProtocolState("not-a-map"))
                .isInstanceOf(ModelProtocolStateException.class)
                .hasMessageContaining("must be a map");

        assertThatThrownBy(() -> ModelProtocolState.deserializeProtocolState(Map.of()))
                .isInstanceOf(ModelProtocolStateException.class)
                .hasMessageContaining("requires schemaVersion and adapter");
    }

    @Test
    void mergesDuplicateSignaturesWithoutDuplicatingThem() {
        ModelProtocolState first = new ModelProtocolState(
                1, "google-genai", Map.of("thoughtSignatures", List.of("a", "b")), List.of());
        ModelProtocolState second = new ModelProtocolState(
                1, "google-genai", Map.of("thoughtSignatures", List.of("b", "c")), List.of());

        assertThat(first.merge(second).messageExtensions().get("thoughtSignatures"))
                .isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    void rejectsConflictingAdaptersAndToolCallIds() {
        ModelProtocolState google = new ModelProtocolState(
                1, "google-genai", Map.of(), List.of());
        ModelProtocolState other = new ModelProtocolState(
                1, "other-provider", Map.of("opaque", "value"), List.of());

        assertThatThrownBy(() -> google.merge(other))
                .isInstanceOf(ModelProtocolStateException.class)
                .hasMessageContaining("different adapters");

        ModelProtocolState firstCall = new ModelProtocolState(
                1, "google-genai", Map.of(), List.of(
                        new ToolCallProtocolState(0, "call-1", Map.of("opaque", "a"))));
        ModelProtocolState conflictingCall = new ModelProtocolState(
                1, "google-genai", Map.of(), List.of(
                        new ToolCallProtocolState(0, "call-2", Map.of("opaque", "a"))));

        assertThatThrownBy(() -> firstCall.merge(conflictingCall))
                .isInstanceOf(ModelProtocolStateException.class)
                .hasMessageContaining("tool-call id");
    }
}
