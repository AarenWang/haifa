package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.discount.DiscountStrategy;
import org.wrj.haifa.designpattern.orderpipeline.strategy.discount.DiscountStrategyRegistry;

/**
 * 折扣处理器 - 职责链中的折扣计算环节
 * 
 * <p>根据用户等级选择对应的折扣策略进行折扣计算</p>
 * <p>这是"策略模式 + 职责链模式"结合的典型示例</p>
 * 
 * @author wrj
 */
@Component
@Order(20) // 在定价之后执行
public class DiscountHandler implements OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(DiscountHandler.class);

    private final DiscountStrategyRegistry registry;

    public DiscountHandler(DiscountStrategyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(OrderContext ctx) {
        String userTier = ctx.getRequest().getUserTier();

        // 根据用户等级选择折扣策略
        DiscountStrategy strategy = registry.getRequired(userTier);

        // 调用策略计算折扣
        int discountCents = strategy.calcDiscountCents(ctx);
        ctx.setDiscountCents(discountCents);

        log.info("DiscountHandler: User tier [{}] -> Strategy [{}] -> Discount {} cents",
                userTier, strategy.getClass().getSimpleName(), discountCents);
    }
}
