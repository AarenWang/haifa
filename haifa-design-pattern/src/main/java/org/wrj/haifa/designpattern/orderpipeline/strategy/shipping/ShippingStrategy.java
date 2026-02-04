package org.wrj.haifa.designpattern.orderpipeline.strategy.shipping;

import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.KeyedStrategy;

/**
 * 运费策略接口 - 根据不同国家/地区计算运费
 * 
 * @author wrj
 */
public interface ShippingStrategy extends KeyedStrategy {

    /**
     * 计算运费
     * 
     * @param ctx 订单上下文
     * @return 运费（单位：分）
     */
    int calcShippingCents(OrderContext ctx);
}
