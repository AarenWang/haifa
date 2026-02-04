package org.wrj.haifa.designpattern.orderpolicyrule.engine;

import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;

/**
 * 单行商品的折扣决策结果。
 */
public final class ItemPolicyDecision {

    private final LineItem item;
    private final PolicyResult policyResult;

    public ItemPolicyDecision(LineItem item, PolicyResult policyResult) {
        this.item = item;
        this.policyResult = policyResult;
    }

    public LineItem getItem() {
        return item;
    }

    public PolicyResult getPolicyResult() {
        return policyResult;
    }
}
