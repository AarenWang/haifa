package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 满 100 减 20 券。
 */
public class Coupon100Minus20Rule implements OrderDiscountRule {

    @Override
    public String id() {
        return "C100_MINUS_20";
    }

    @Override
    public String mutexGroup() {
        return "ORDER_COUPON";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean stackable() {
        return false;
    }

    @Override
    public boolean supports(OrderContext ctx) {
        return "C100-20".equalsIgnoreCase(ctx.getRequest().getCouponCode());
    }

    @Override
    public int calcDiscountCents(OrderContext ctx) {
        return ctx.getItemsAfterItemDiscCents() >= 10000 ? 2000 : 0;
    }
}
