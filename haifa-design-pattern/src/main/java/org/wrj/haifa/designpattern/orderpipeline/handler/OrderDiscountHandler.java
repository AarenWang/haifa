package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount.OrderDiscountStrategy;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;

import java.util.List;

/**
 * 订单级折扣处理器：选择一个互斥策略应用一次
 * 记录订单折扣明细用于审计
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
        OrderDiscountStrategy appliedStrategy = null;

        for (OrderDiscountStrategy s : strategies) {
            if (s.supports(ctx)) {
                discount = s.calcOrderDiscountCents(ctx);
                appliedStrategy = s;
                break;
            }
        }

        int capped = Math.min(discount, ctx.getItemsAfterItemDiscCents());
        int finalDiscount = Math.max(0, capped);
        ctx.setOrderDiscountCents(finalDiscount);
        ctx.setDiscountCents(ctx.getItemDiscountCents() + ctx.getOrderDiscountCents());

        // 记录订单折扣明细
        if (appliedStrategy != null && finalDiscount > 0) {
            DiscountEntry entry = appliedStrategy.createDiscountEntry(ctx, finalDiscount);
            ctx.addOrderDiscountEntry(entry);
        }
    }
}
