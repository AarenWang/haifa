package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingStrategy;
import org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingStrategyRegistry;

/**
 * 运费处理器 - 职责链中的运费计算环节
 * 
 * <p>根据订单的目的国家选择对应的运费策略进行运费计算</p>
 * <p>这是"策略模式 + 职责链模式"结合的典型示例</p>
 * 
 * @author wrj
 */
@Component
@Order(40) // 在所有折扣之后执行
public class ShippingHandler implements OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(ShippingHandler.class);

    private final ShippingStrategyRegistry registry;

    public ShippingHandler(ShippingStrategyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(OrderContext ctx) {
        String country = ctx.getRequest().getCountry();

        // 根据国家选择运费策略
        ShippingStrategy strategy = registry.getRequired(country);

        // 调用策略计算运费
        int shippingCents = strategy.calcShippingCents(ctx);
        ctx.setShippingCents(shippingCents);

        log.info("ShippingHandler: Country [{}] -> Strategy [{}] -> Shipping {} cents",
                country, strategy.getClass().getSimpleName(), shippingCents);
    }
}
