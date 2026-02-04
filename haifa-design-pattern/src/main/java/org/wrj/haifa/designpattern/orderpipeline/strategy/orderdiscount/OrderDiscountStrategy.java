package org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount;

import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 订单级折扣策略（互斥）
 */
public interface OrderDiscountStrategy {
    boolean supports(OrderContext ctx);
    int calcOrderDiscountCents(OrderContext ctx);
    default String codeType() { return "ORDER_DISCOUNT"; }
}
