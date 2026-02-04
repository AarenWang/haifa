package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Policy 注册表：按 key 管理不同的折扣策略。
 */
public class PolicyRegistry<T extends KeyedPolicy> {

    private final Map<String, T> policyMap;

    public PolicyRegistry(List<T> policies) {
        this.policyMap = policies.stream()
                .collect(Collectors.toMap(
                        KeyedPolicy::key,
                        Function.identity(),
                        (existing, replacement) -> {
                            throw new IllegalStateException(
                                    String.format("Duplicate policy key: %s", existing.key()));
                        }));
    }

    public T getRequired(String key) {
        T policy = policyMap.get(key);
        if (policy == null) {
            throw new IllegalArgumentException(
                    String.format("No policy found for key: %s. Available keys: %s", key, policyMap.keySet()));
        }
        return policy;
    }

    public Optional<T> get(String key) {
        return Optional.ofNullable(policyMap.get(key));
    }

    public boolean contains(String key) {
        return policyMap.containsKey(key);
    }
}
