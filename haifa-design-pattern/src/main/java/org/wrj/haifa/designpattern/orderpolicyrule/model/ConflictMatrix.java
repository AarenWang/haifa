package org.wrj.haifa.designpattern.orderpolicyrule.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 跨组冲突矩阵：用于表达“组A与组B不可同时使用”。
 */
public final class ConflictMatrix {

    private final Map<String, Set<String>> conflicts = new HashMap<>();

    public ConflictMatrix addConflict(String groupA, String groupB) {
        if (groupA == null || groupB == null) {
            return this;
        }
        conflicts.computeIfAbsent(groupA, key -> new HashSet<>()).add(groupB);
        conflicts.computeIfAbsent(groupB, key -> new HashSet<>()).add(groupA);
        return this;
    }

    public boolean isConflict(String groupA, String groupB) {
        if (groupA == null || groupB == null) {
            return false;
        }
        return conflicts.getOrDefault(groupA, Set.of()).contains(groupB);
    }

    public Map<String, Set<String>> getConflicts() {
        Map<String, Set<String>> snapshot = new HashMap<>();
        conflicts.forEach((key, value) -> snapshot.put(key, new HashSet<>(value)));
        return Collections.unmodifiableMap(snapshot);
    }
}
