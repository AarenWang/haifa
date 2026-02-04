package org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

@Component
@Order(20)
public class DiscountVIPRule implements ItemDiscountRule {

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return "VIP".equalsIgnoreCase(ctx.getRequest().getUserTier());
    }

    @Override
    public int calcDiscountCents(OrderContext ctx, LineItem item) {
        int lineRaw = item.getRawLineCents();
        return (int) Math.round(lineRaw * 0.05); // VIP 95 折 => 5% off
    }

    @Override
    public String tag() { return "VIP"; }

    @Override
    public int priority() { return 20; }
}
