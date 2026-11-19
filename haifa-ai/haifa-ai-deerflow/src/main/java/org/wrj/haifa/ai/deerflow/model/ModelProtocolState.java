package org.wrj.haifa.ai.deerflow.model;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

public record ModelProtocolState(
        int schemaVersion,
        String adapter,
        Map<String, Object> messageExtensions,
        List<ToolCallProtocolState> toolCallExtensions
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static ModelProtocolState empty() {
        return new ModelProtocolState(CURRENT_SCHEMA_VERSION, "none", Map.of(), List.of());
    }

    public ModelProtocolState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new ModelProtocolStateException("Unsupported model protocol state schema version: " + schemaVersion);
        }
        if (adapter == null || adapter.isBlank()) {
            throw new ModelProtocolStateException("Model protocol state adapter must not be blank");
        }
        messageExtensions = messageExtensions == null ? Map.of() : Map.copyOf(messageExtensions);
        toolCallExtensions = toolCallExtensions == null ? List.of() : List.copyOf(toolCallExtensions);
    }

    public boolean isEmpty() {
        return "none".equals(adapter);
    }

    public ModelProtocolState merge(ModelProtocolState other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        if (!this.adapter().equals(other.adapter())) {
            throw new ModelProtocolStateException(
                    "Cannot merge model protocol states from different adapters");
        }

        Map<String, Object> mergedMessageExt = new HashMap<>(this.messageExtensions);
        other.messageExtensions().forEach((key, val) -> {
            if (mergedMessageExt.containsKey(key)) {
                Object thisVal = mergedMessageExt.get(key);
                if (thisVal instanceof List && val instanceof List) {
                    List<Object> mergedList = new ArrayList<>((List<?>) thisVal);
                    for (Object item : (List<?>) val) {
                        if (!mergedList.contains(item)) {
                            mergedList.add(item);
                        }
                    }
                    mergedMessageExt.put(key, mergedList);
                }
                else if (!Objects.equals(thisVal, val)) {
                    throw new ModelProtocolStateException(
                            "Conflicting model protocol message extension: " + key);
                }
            }
            else {
                mergedMessageExt.put(key, val);
            }
        });

        List<ToolCallProtocolState> mergedToolCalls = new ArrayList<>(this.toolCallExtensions);
        for (ToolCallProtocolState incoming : other.toolCallExtensions()) {
            int existingIndex = findToolCallState(mergedToolCalls, incoming);
            if (existingIndex < 0) {
                mergedToolCalls.add(incoming);
                continue;
            }
            ToolCallProtocolState existing = mergedToolCalls.get(existingIndex);
            if (existing.toolCallId() != null && incoming.toolCallId() != null
                    && !existing.toolCallId().equals(incoming.toolCallId())) {
                throw new ModelProtocolStateException(
                        "Conflicting model protocol tool-call id at ordinal " + incoming.ordinal());
            }
            Map<String, Object> mergedExtensions = new LinkedHashMap<>(existing.extensions());
            incoming.extensions().forEach((key, value) -> {
                if (mergedExtensions.containsKey(key)
                        && !Objects.equals(mergedExtensions.get(key), value)) {
                    throw new ModelProtocolStateException(
                            "Conflicting model protocol tool-call extension: " + key);
                }
                mergedExtensions.putIfAbsent(key, value);
            });
            String toolCallId = existing.toolCallId() != null ? existing.toolCallId() : incoming.toolCallId();
            mergedToolCalls.set(existingIndex,
                    new ToolCallProtocolState(existing.ordinal(), toolCallId, mergedExtensions));
        }

        return new ModelProtocolState(this.schemaVersion, this.adapter, mergedMessageExt, mergedToolCalls);
    }

    private static int findToolCallState(List<ToolCallProtocolState> states, ToolCallProtocolState target) {
        for (int i = 0; i < states.size(); i++) {
            ToolCallProtocolState candidate = states.get(i);
            if (candidate.ordinal() == target.ordinal()) {
                return i;
            }
            if (candidate.toolCallId() != null && target.toolCallId() != null
                    && candidate.toolCallId().equals(target.toolCallId())) {
                return i;
            }
        }
        return -1;
    }

    public static Map<String, Object> serializeProtocolState(ModelProtocolState state) {
        if (state == null || state.isEmpty()) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("schemaVersion", state.schemaVersion());
        map.put("adapter", state.adapter());
        map.put("messageExtensions", state.messageExtensions());
        
        List<Map<String, Object>> toolCallList = new ArrayList<>();
        for (ToolCallProtocolState tc : state.toolCallExtensions()) {
            Map<String, Object> tcMap = new HashMap<>();
            tcMap.put("ordinal", tc.ordinal());
            tcMap.put("toolCallId", tc.toolCallId());
            tcMap.put("extensions", tc.extensions());
            toolCallList.add(tcMap);
        }
        map.put("toolCallExtensions", toolCallList);
        return map;
    }

    public static ModelProtocolState deserializeProtocolState(Object obj) {
        if (obj == null) {
            return ModelProtocolState.empty();
        }
        if (!(obj instanceof Map<?, ?> raw)) {
            throw new ModelProtocolStateException("Model protocol state must be a map");
        }
        try {
            if (!raw.containsKey("schemaVersion") || !raw.containsKey("adapter")) {
                throw new ModelProtocolStateException(
                        "Model protocol state requires schemaVersion and adapter");
            }
            int schemaVersion = numberValue(raw.get("schemaVersion"), CURRENT_SCHEMA_VERSION, "schemaVersion");
            String adapter = stringValue(raw.get("adapter"), null, "adapter");
            Map<String, Object> messageExtensions = stringObjectMap(
                    raw.get("messageExtensions"), "messageExtensions");

            List<ToolCallProtocolState> toolCallExtensions = new ArrayList<>();
            Object toolCallsObj = raw.get("toolCallExtensions");
            if (toolCallsObj instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> tcMap)) {
                        throw new ModelProtocolStateException("toolCallExtensions entries must be maps");
                    }
                    if (!tcMap.containsKey("ordinal")) {
                        throw new ModelProtocolStateException("toolCallExtensions entries require ordinal");
                    }
                    int ordinal = numberValue(tcMap.get("ordinal"), 0, "ordinal");
                    String toolCallId = stringValue(tcMap.get("toolCallId"), null, "toolCallId");
                    Map<String, Object> extensions = stringObjectMap(tcMap.get("extensions"), "extensions");
                    toolCallExtensions.add(new ToolCallProtocolState(ordinal, toolCallId, extensions));
                }
            }
            else if (toolCallsObj != null) {
                throw new ModelProtocolStateException("toolCallExtensions must be a list");
            }
            return new ModelProtocolState(schemaVersion, adapter, messageExtensions, toolCallExtensions);
        }
        catch (ModelProtocolStateException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            throw new ModelProtocolStateException("Invalid model protocol state", ex);
        }
    }

    private static int numberValue(Object value, int defaultValue, String field) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new ModelProtocolStateException(field + " must be a number");
    }

    private static String stringValue(Object value, String defaultValue, String field) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String string) {
            return string;
        }
        throw new ModelProtocolStateException(field + " must be a string");
    }

    private static Map<String, Object> stringObjectMap(Object value, String field) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new ModelProtocolStateException(field + " must be a map");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, item) -> {
            if (!(key instanceof String stringKey)) {
                throw new ModelProtocolStateException(field + " keys must be strings");
            }
            result.put(stringKey, item);
        });
        return result;
    }
}
