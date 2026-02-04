package org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

@Component
@Order(30)
public class DiscountNormalRule implements ItemDiscountRule {

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return true; // 普通用户无特别折扣，但规则存在以便扩展
    }

    @Override
    public int calcDiscountCents(OrderContext ctx, LineItem item) {
        return 0;
    }

    @Override
    public String tag() { return "NORMAL"; }

    @Override
    public int priority() { return 30; }
}
