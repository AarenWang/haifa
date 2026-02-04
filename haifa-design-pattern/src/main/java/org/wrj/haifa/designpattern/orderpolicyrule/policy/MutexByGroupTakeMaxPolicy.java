package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;
import org.wrj.haifa.designpattern.orderpolicyrule.model.RejectedCandidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组内互斥取最大，跨组叠加。
 */
public class MutexByGroupTakeMaxPolicy implements KeyedPolicy {

    private static final String DEFAULT_GROUP = "__DEFAULT__";

    private static final Comparator<DiscountCandidate> BEST_FIRST =
            Comparator.comparingInt(DiscountCandidate::getDiscountCents)
                    .thenComparingInt(DiscountCandidate::getPriority);

    @Override
    public String key() {
        return "MUTEX_BY_GROUP_TAKE_MAX";
    }

    @Override
    public PolicyResult apply(int baseCents, List<DiscountCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return PolicyResult.empty();
        }
        Map<String, List<DiscountCandidate>> groups = candidates.stream()
                .collect(Collectors.groupingBy(candidate ->
                        candidate.getMutexGroup() == null ? DEFAULT_GROUP : candidate.getMutexGroup()));

        List<DiscountCandidate> chosen = new ArrayList<>();
        List<RejectedCandidate> rejected = new ArrayList<>();

        for (List<DiscountCandidate> group : groups.values()) {
            DiscountCandidate best = group.stream().max(BEST_FIRST).orElse(null);
            if (best != null) {
                chosen.add(best);
                group.stream()
                        .filter(candidate -> candidate != best)
                        .map(candidate -> new RejectedCandidate(candidate, "GROUP_MUTEX_REJECTED"))
                        .forEach(rejected::add);
            }
        }

        int sum = chosen.stream().mapToInt(DiscountCandidate::getDiscountCents).sum();
        boolean capped = sum > baseCents;
        int finalDiscount = Math.min(sum, baseCents);
        return new PolicyResult(finalDiscount, chosen, rejected, capped);
    }
}
