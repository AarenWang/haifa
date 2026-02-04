package org.wrj.haifa.designpattern.orderpolicyrule.engine;

import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.DiscountPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.ItemDiscountRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.OrderDiscountRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则 + Policy 的执行引擎：产候选 -> 选择 -> 写回订单上下文。
 */
public class OrderPolicyEngine {

    private final List<ItemDiscountRule> itemRules;
    private final List<OrderDiscountRule> orderRules;
    private final DiscountPolicy itemPolicy;
    private final DiscountPolicy orderPolicy;

    public OrderPolicyEngine(List<ItemDiscountRule> itemRules,
                             List<OrderDiscountRule> orderRules,
                             DiscountPolicy itemPolicy,
                             DiscountPolicy orderPolicy) {
        this.itemRules = itemRules;
        this.orderRules = orderRules;
        this.itemPolicy = itemPolicy;
        this.orderPolicy = orderPolicy;
    }

    public PolicyExecutionResult execute(OrderContext ctx) {
        List<ItemPolicyDecision> itemDecisions = applyItemDiscounts(ctx);
        PolicyResult orderResult = applyOrderDiscounts(ctx);
        return new PolicyExecutionResult(itemDecisions, orderResult);
    }

    private List<ItemPolicyDecision> applyItemDiscounts(OrderContext ctx) {
        List<ItemPolicyDecision> decisions = new ArrayList<>();
        int subtotal = 0;
        int afterItemDiscount = 0;
        int itemDiscountTotal = 0;

        if (!ctx.getRequest().hasItems()) {
            int raw = ctx.getRequest().getAmountCents();
            ctx.setItemsSubtotalCents(raw);
            ctx.setItemsAfterItemDiscCents(raw);
            ctx.setItemDiscountCents(0);
            ctx.setBasePriceCents(raw);
            return decisions;
        }

        for (LineItem item : ctx.getRequest().getItems()) {
            int lineRaw = item.getRawLineCents();
            subtotal += lineRaw;

            List<DiscountCandidate> candidates = new ArrayList<>();
            for (ItemDiscountRule rule : itemRules) {
                if (rule.supports(ctx, item)) {
                    DiscountCandidate candidate = rule.evaluate(ctx, item);
                    if (candidate.getDiscountCents() > 0) {
                        candidates.add(candidate);
                    }
                }
            }

            PolicyResult result = itemPolicy.apply(lineRaw, candidates);
            int applied = Math.min(result.getAppliedDiscountCents(), lineRaw);
            item.setItemDiscountCents(applied);
            item.setFinalLineCents(lineRaw - applied);

            for (DiscountCandidate candidate : result.getChosenCandidates()) {
                DiscountEntry entry = candidate.getEntry();
                if (entry != null && candidate.getDiscountCents() > 0) {
                    item.getDiscountTags().add(candidate.getId());
                    item.getItemDiscountEntries().add(entry);
                    ctx.addItemDiscountEntry(entry);
                }
            }

            decisions.add(new ItemPolicyDecision(item, result));
            afterItemDiscount += item.getFinalLineCents();
            itemDiscountTotal += applied;
        }

        ctx.setItemsSubtotalCents(subtotal);
        ctx.setItemsAfterItemDiscCents(afterItemDiscount);
        ctx.setItemDiscountCents(itemDiscountTotal);
        ctx.setBasePriceCents(subtotal);
        return decisions;
    }

    private PolicyResult applyOrderDiscounts(OrderContext ctx) {
        List<DiscountCandidate> candidates = new ArrayList<>();
        for (OrderDiscountRule rule : orderRules) {
            if (rule.supports(ctx)) {
                DiscountCandidate candidate = rule.evaluate(ctx);
                if (candidate.getDiscountCents() > 0) {
                    candidates.add(candidate);
                }
            }
        }

        PolicyResult result = orderPolicy.apply(ctx.getItemsAfterItemDiscCents(), candidates);
        int capped = Math.min(result.getAppliedDiscountCents(), ctx.getItemsAfterItemDiscCents());
        int finalDiscount = Math.max(0, capped);

        ctx.setOrderDiscountCents(finalDiscount);
        ctx.setDiscountCents(ctx.getItemDiscountCents() + finalDiscount);

        for (DiscountCandidate candidate : result.getChosenCandidates()) {
            DiscountEntry entry = candidate.getEntry();
            if (entry != null && candidate.getDiscountCents() > 0) {
                ctx.addOrderDiscountEntry(entry);
            }
        }

        return result;
    }
}
