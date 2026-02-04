package org.wrj.haifa.designpattern.orderpipeline.strategy.discount;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * SVIP 超级会员折扣策略
 * 
 * <p>示例：SVIP 用户享受 90 折优惠（即折扣 10%）</p>
 * 
 * @author wrj
 */
@Component
public class DiscountSVIP implements DiscountStrategy {

    private static final double DISCOUNT_RATE = 0.10; // 10% 折扣

    @Override
    public String key() {
        return "SVIP";
    }

    @Override
    public int calcDiscountCents(OrderContext ctx) {
        // SVIP 用户 90 折，即折扣 10%
        return (int) Math.round(ctx.getBasePriceCents() * DISCOUNT_RATE);
    }
}
