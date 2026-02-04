package org.wrj.haifa.designpattern.orderpolicyrule.engine;

import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;

import java.util.Collections;
import java.util.List;

/**
 * 订单级 Policy 执行结果汇总。
 */
public final class PolicyExecutionResult {

    private final List<ItemPolicyDecision> itemDecisions;
    private final PolicyResult orderPolicyResult;

    public PolicyExecutionResult(List<ItemPolicyDecision> itemDecisions, PolicyResult orderPolicyResult) {
        this.itemDecisions = Collections.unmodifiableList(itemDecisions);
        this.orderPolicyResult = orderPolicyResult;
    }

    public List<ItemPolicyDecision> getItemDecisions() {
        return itemDecisions;
    }

    public PolicyResult getOrderPolicyResult() {
        return orderPolicyResult;
    }
}
