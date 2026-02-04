package org.wrj.haifa.designpattern.orderpipeline.strategy.shipping;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 美国运费策略
 * 
 * <p>示例：美国固定运费 15 美元（1500 美分）</p>
 * 
 * @author wrj
 */
@Component
public class ShippingUS implements ShippingStrategy {

    private static final int FIXED_SHIPPING_CENTS = 1500; // 15 USD

    @Override
    public String key() {
        return "US";
    }

    @Override
    public int calcShippingCents(OrderContext ctx) {
        // 示例：美国固定 15 美元
        // 实际业务中可能根据州、重量、快递方式等计算
        return FIXED_SHIPPING_CENTS;
    }
}
