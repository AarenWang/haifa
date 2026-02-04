package org.wrj.haifa.designpattern.orderpipeline.strategy.discount;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * VIP 用户折扣策略
 * 
 * <p>示例：VIP 用户享受 95 折优惠（即折扣 5%）</p>
 * 
 * @author wrj
 */
@Component
public class DiscountVIP implements DiscountStrategy {

    private static final double DISCOUNT_RATE = 0.05; // 5% 折扣

    @Override
    public String key() {
        return "VIP";
    }

    @Override
    public int calcDiscountCents(OrderContext ctx) {
        // VIP 用户 95 折，即折扣 5%
        return (int) Math.round(ctx.getBasePriceCents() * DISCOUNT_RATE);
    }
}
