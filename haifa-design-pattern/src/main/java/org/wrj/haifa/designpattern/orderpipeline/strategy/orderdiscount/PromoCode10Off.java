package org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

@Component
@Order(20)
public class PromoCode10Off implements OrderDiscountStrategy {

    @Override
    public boolean supports(OrderContext ctx) {
        return "OFF10".equalsIgnoreCase(ctx.getRequest().getCouponCode());
    }

    @Override
    public int calcOrderDiscountCents(OrderContext ctx) {
        return (int) Math.round(ctx.getItemsAfterItemDiscCents() * 0.10);
    }

    @Override
    public String codeType() { return "PROMO_CODE"; }
}
