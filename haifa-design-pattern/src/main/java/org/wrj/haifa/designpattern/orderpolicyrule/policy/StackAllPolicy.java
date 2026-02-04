package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;
import java.util.List;

/**
 * 全叠加：所有候选折扣相加后封顶。
 */
public class StackAllPolicy implements KeyedPolicy {

    @Override
    public String key() {
        return "STACK_ALL";
    }

    @Override
    public PolicyResult apply(int baseCents, List<DiscountCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return PolicyResult.empty();
        }
        int sum = candidates.stream().mapToInt(DiscountCandidate::getDiscountCents).sum();
        boolean capped = sum > baseCents;
        int finalDiscount = Math.min(sum, baseCents);
        return new PolicyResult(finalDiscount, candidates, List.of(), capped);
    }
}
