package org.wrj.haifa.designpattern.orderpipeline.strategy.discount;

import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.KeyedStrategy;

/**
 * 折扣策略接口 - 根据不同用户等级计算折扣
 * 
 * @author wrj
 */
public interface DiscountStrategy extends KeyedStrategy {

    /**
     * 计算折扣金额
     * 
     * @param ctx 订单上下文
     * @return 折扣金额（单位：分）
     */
    int calcDiscountCents(OrderContext ctx);
}
