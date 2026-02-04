package org.wrj.haifa.designpattern.orderpipeline.strategy.discount;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.strategy.StrategyRegistry;

import java.util.List;

/**
 * 折扣策略注册表
 * 
 * <p>收集所有 {@link DiscountStrategy} 实现并提供按用户等级 key 查找的能力</p>
 * 
 * @author wrj
 */
@Component
public class DiscountStrategyRegistry extends StrategyRegistry<DiscountStrategy> {

    public DiscountStrategyRegistry(List<DiscountStrategy> strategies) {
        super(strategies, "Discount");
    }
}
