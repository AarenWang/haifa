package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountScope;

/**
 * 订单级折扣规则：只产出候选折扣。
 */
public interface OrderDiscountRule {

    String id();

    default String mutexGroup() {
        return null;
    }

    default int priority() {
        return 100;
    }

    default boolean stackable() {
        return true;
    }

    boolean supports(OrderContext ctx);

    int calcDiscountCents(OrderContext ctx);

    default DiscountCandidate evaluate(OrderContext ctx) {
        int discountCents = calcDiscountCents(ctx);
        DiscountEntry entry = createDiscountEntry(ctx, discountCents);
        return DiscountCandidate.builder()
                .id(id())
                .scope(DiscountScope.ORDER)
                .mutexGroup(mutexGroup())
                .priority(priority())
                .discountCents(discountCents)
                .stackable(stackable())
                .meta("ruleName", getClass().getSimpleName())
                .entry(entry)
                .build();
    }

    default DiscountEntry createDiscountEntry(OrderContext ctx, int amountCents) {
        return DiscountEntry.builder()
                .amountCents(amountCents)
                .source("ORDER_" + id())
                .ruleName(getClass().getSimpleName())
                .couponId(ctx.getRequest().getCouponCode())
                .build();
    }
}
