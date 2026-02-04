package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount.OrderDiscountStrategy;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;

import java.util.List;

/**
 * 订单级折扣处理器：选择一个互斥策略应用一次
 */
@Component
@Order(30)
public class OrderDiscountHandler implements OrderHandler {

    private final List<OrderDiscountStrategy> strategies;

    public OrderDiscountHandler(List<OrderDiscountStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public void handle(OrderContext ctx) {
        int discount = 0;
        for (OrderDiscountStrategy s : strategies) {
            if (s.supports(ctx)) {
                discount = s.calcOrderDiscountCents(ctx);
                break;
            }
        }
        int capped = Math.min(discount, ctx.getItemsAfterItemDiscCents());
        ctx.setOrderDiscountCents(Math.max(0, capped));
        ctx.setDiscountCents(ctx.getItemDiscountCents() + ctx.getOrderDiscountCents());
    }
}
