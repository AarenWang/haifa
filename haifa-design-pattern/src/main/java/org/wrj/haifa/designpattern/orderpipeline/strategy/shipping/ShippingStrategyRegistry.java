package org.wrj.haifa.designpattern.orderpipeline.strategy.shipping;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.strategy.StrategyRegistry;

import java.util.List;

/**
 * 运费策略注册表
 * 
 * <p>收集所有 {@link ShippingStrategy} 实现并提供按国家 key 查找的能力</p>
 * 
 * @author wrj
 */
@Component
public class ShippingStrategyRegistry extends StrategyRegistry<ShippingStrategy> {

    public ShippingStrategyRegistry(List<ShippingStrategy> strategies) {
        super(strategies, "Shipping");
    }
}
