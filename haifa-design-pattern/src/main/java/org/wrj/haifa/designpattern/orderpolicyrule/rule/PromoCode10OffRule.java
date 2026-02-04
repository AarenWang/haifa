package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 9 折优惠码（10% off）。
 */
public class PromoCode10OffRule implements OrderDiscountRule {

    @Override
    public String id() {
        return "PROMO_10_OFF";
    }

    @Override
    public String mutexGroup() {
        return "PROMO_CODE";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public boolean stackable() {
        return false;
    }

    @Override
    public boolean supports(OrderContext ctx) {
        return "PROMO10".equalsIgnoreCase(ctx.getRequest().getCouponCode());
    }

    @Override
    public int calcDiscountCents(OrderContext ctx) {
        int base = ctx.getItemsAfterItemDiscCents();
        return (int) Math.round(base * 0.10);
    }
}
