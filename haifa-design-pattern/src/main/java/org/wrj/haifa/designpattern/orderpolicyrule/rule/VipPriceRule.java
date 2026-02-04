package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 会员折扣：VIP 享受 95 折（5% off）。
 */
public class VipPriceRule implements ItemDiscountRule {

    @Override
    public String id() {
        return "VIP_PRICE";
    }

    @Override
    public String mutexGroup() {
        return "PRICE_PROMO";
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
    public boolean supports(OrderContext ctx, LineItem item) {
        return "VIP".equalsIgnoreCase(ctx.getRequest().getUserTier());
    }

    @Override
    public int calcDiscountCents(OrderContext ctx, LineItem item) {
        int lineRaw = item.getRawLineCents();
        return (int) Math.round(lineRaw * 0.05);
    }
}
