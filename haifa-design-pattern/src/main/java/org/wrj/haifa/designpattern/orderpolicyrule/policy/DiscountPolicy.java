package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;

import java.util.List;

/**
 * 折扣策略：将候选折扣合成为最终折扣。
 */
public interface DiscountPolicy {

    PolicyResult apply(int baseCents, List<DiscountCandidate> candidates);
}
