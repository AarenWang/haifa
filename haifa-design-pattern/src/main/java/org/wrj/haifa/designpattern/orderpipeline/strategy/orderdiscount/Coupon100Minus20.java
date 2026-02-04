package org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

@Component
@Order(10)
public class Coupon100Minus20 implements OrderDiscountStrategy {

    @Override
    public boolean supports(OrderContext ctx) {
        return "C100-20".equalsIgnoreCase(ctx.getRequest().getCouponCode());
    }

    @Override
    public int calcOrderDiscountCents(OrderContext ctx) {
        return ctx.getItemsAfterItemDiscCents() >= 10000 ? 2000 : 0;
    }

    @Override
    public String codeType() { return "COUPON"; }
}
