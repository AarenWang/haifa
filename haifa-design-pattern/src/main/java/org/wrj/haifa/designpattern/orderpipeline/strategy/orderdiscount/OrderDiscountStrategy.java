package org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount;

import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 订单级折扣策略（互斥）
 */
public interface OrderDiscountStrategy {
    boolean supports(OrderContext ctx);
    int calcOrderDiscountCents(OrderContext ctx);
    default String codeType() { return "ORDER_DISCOUNT"; }

    /**
     * 创建折扣明细（默认实现）
     * 子类可以重写此方法提供更详细的明细信息
     */
    default DiscountEntry createDiscountEntry(OrderContext ctx, int amountCents) {
        return DiscountEntry.builder()
                .amountCents(amountCents)
                .source(codeType())
                .ruleName(this.getClass().getSimpleName())
                .couponId(ctx.getRequest().getCouponCode())
                .build();
    }
}
