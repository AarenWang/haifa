package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;
import org.wrj.haifa.designpattern.orderpolicyrule.model.RejectedCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * 全局取最大：只选择一个最优候选。
 */
public class TakeMaxPolicy implements KeyedPolicy {

    private static final Comparator<DiscountCandidate> BEST_FIRST =
            Comparator.comparingInt(DiscountCandidate::getDiscountCents)
                    .thenComparingInt(DiscountCandidate::getPriority);

    @Override
    public String key() {
        return "TAKE_MAX";
    }

    @Override
    public PolicyResult apply(int baseCents, List<DiscountCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return PolicyResult.empty();
        }
        DiscountCandidate best = candidates.stream().max(BEST_FIRST).orElse(null);
        if (best == null) {
            return PolicyResult.empty();
        }
        int finalDiscount = Math.min(best.getDiscountCents(), baseCents);
        List<RejectedCandidate> rejected = candidates.stream()
                .filter(candidate -> candidate != best)
                .map(candidate -> new RejectedCandidate(candidate, "TAKE_MAX_REJECTED"))
                .toList();
        return new PolicyResult(finalDiscount, List.of(best), rejected, best.getDiscountCents() > baseCents);
    }
}
