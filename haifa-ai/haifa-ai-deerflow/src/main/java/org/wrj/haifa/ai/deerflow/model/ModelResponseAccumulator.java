package org.wrj.haifa.ai.deerflow.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.wrj.haifa.ai.deerflow.model.cache.ModelUsage;
import org.wrj.haifa.ai.deerflow.model.cache.UsageAvailability;

public class ModelResponseAccumulator {
    private final StringBuilder content = new StringBuilder();
    private final List<MutableToolCall> toolCalls = new ArrayList<>();
    private final List<String> invalidToolCalls = new ArrayList<>();
    private String finishReason = null;
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private ModelProtocolState protocolState = ModelProtocolState.empty();
    private ModelUsage usage = ModelUsage.empty();

    public void accumulate(ModelResponse response) {
        if (response == null) {
            return;
        }
        if (response.content() != null && !response.content().isEmpty()) {
            content.append(response.content());
        }
        if (response.toolCalls() != null) {
            mergeToolCalls(response.toolCalls());
        }
        if (response.invalidToolCalls() != null) {
            invalidToolCalls.addAll(response.invalidToolCalls());
        }
        if (response.finishReason() != null) {
            finishReason = response.finishReason();
        }
        if (response.metadata() != null) {
            metadata.putAll(response.metadata());
        }
        if (response.protocolState() != null && !response.protocolState().isEmpty()) {
            protocolState = protocolState.merge(response.protocolState());
        }
        if (response.usage() != null && response.usage().availability() != UsageAvailability.UNAVAILABLE) {
            usage = mergeUsage(usage, response.usage());
        }
    }

    public ModelResponse toResponse() {
        return new ModelResponse(
                content.toString(),
                toolCalls.stream().map(MutableToolCall::toModelToolCall).toList(),
                invalidToolCalls,
                finishReason,
                metadata,
                protocolState,
                usage
        );
    }

    private static ModelUsage mergeUsage(ModelUsage current, ModelUsage incoming) {
        if (incoming == null || incoming.availability() == UsageAvailability.UNAVAILABLE) {
            return current;
        }
        if (current == null || current.availability() == UsageAvailability.UNAVAILABLE) {
            return incoming;
        }
        Long input = incoming.inputTokens() != null ? incoming.inputTokens() : current.inputTokens();
        Long uncached = incoming.uncachedInputTokens() != null ? incoming.uncachedInputTokens() : current.uncachedInputTokens();
        Long output = incoming.outputTokens() != null ? incoming.outputTokens() : current.outputTokens();
        Long total = incoming.totalTokens() != null ? incoming.totalTokens() : current.totalTokens();
        Long cacheRead = incoming.cacheReadInputTokens() != null ? incoming.cacheReadInputTokens() : current.cacheReadInputTokens();
        Long cacheWrite = incoming.cacheWriteInputTokens() != null ? incoming.cacheWriteInputTokens() : current.cacheWriteInputTokens();
        Long reasoning = incoming.reasoningTokens() != null ? incoming.reasoningTokens() : current.reasoningTokens();
        String provider = hasText(incoming.provider()) ? incoming.provider() : current.provider();
        String model = hasText(incoming.model()) ? incoming.model() : current.model();
        UsageAvailability avail = incoming.availability() != UsageAvailability.UNAVAILABLE ? incoming.availability() : current.availability();

        Map<String, Long> details = new HashMap<>(current.providerDetails() != null ? current.providerDetails() : Map.of());
        if (incoming.providerDetails() != null) {
            details.putAll(incoming.providerDetails());
        }
        return new ModelUsage(input, uncached, output, total, cacheRead, cacheWrite, reasoning, provider, model, avail, details);
    }

    private void mergeToolCalls(List<ModelToolCall> incomingCalls) {
        for (int ordinal = 0; ordinal < incomingCalls.size(); ordinal++) {
            ModelToolCall incoming = incomingCalls.get(ordinal);
            int existingIndex = findExistingToolCall(incoming, ordinal);
            if (existingIndex < 0) {
                toolCalls.add(MutableToolCall.from(incoming));
            }
            else {
                toolCalls.get(existingIndex).merge(incoming);
            }
        }
    }

    private int findExistingToolCall(ModelToolCall incoming, int ordinal) {
        if (hasText(incoming.id())) {
            for (int i = 0; i < toolCalls.size(); i++) {
                if (incoming.id().equals(toolCalls.get(i).id)) {
                    return i;
                }
            }
        }
        if (ordinal < toolCalls.size()) {
            MutableToolCall candidate = toolCalls.get(ordinal);
            if (!hasText(incoming.id()) || !hasText(candidate.id) || incoming.id().equals(candidate.id)) {
                return ordinal;
            }
        }
        return -1;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static String mergeStable(String current, String incoming, String field) {
        if (!hasText(incoming)) {
            return current;
        }
        if (!hasText(current)) {
            return incoming;
        }
        if (!current.equals(incoming)) {
            throw new ModelProtocolStateException("Conflicting streamed tool-call " + field);
        }
        return current;
    }

    private static String mergeFragment(String current, String incoming) {
        if (!hasText(incoming)) {
            return current;
        }
        if (!hasText(current)) {
            return incoming;
        }
        if (current.equals(incoming) || current.startsWith(incoming)) {
            return current;
        }
        if (incoming.startsWith(current)) {
            return incoming;
        }
        return current + incoming;
    }

    private static final class MutableToolCall {
        private String id;
        private String name;
        private String arguments;
        private String type;

        private static MutableToolCall from(ModelToolCall call) {
            MutableToolCall result = new MutableToolCall();
            result.id = call.id();
            result.name = call.name();
            result.arguments = call.arguments();
            result.type = call.type();
            return result;
        }

        private void merge(ModelToolCall incoming) {
            id = mergeStable(id, incoming.id(), "id");
            name = mergeFragment(name, incoming.name());
            arguments = mergeFragment(arguments, incoming.arguments());
            type = mergeStable(type, incoming.type(), "type");
        }

        private ModelToolCall toModelToolCall() {
            return new ModelToolCall(id, name, arguments, type);
        }
    }
}
