package org.wrj.haifa.designpattern.orderpipeline.strategy.shipping;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 中国大陆运费策略
 * 
 * <p>示例：国内固定运费 8 元（800 分）</p>
 * 
 * @author wrj
 */
@Component
public class ShippingCN implements ShippingStrategy {

    private static final int FIXED_SHIPPING_CENTS = 800; // 8 元

    @Override
    public String key() {
        return "CN";
    }

    @Override
    public int calcShippingCents(OrderContext ctx) {
        // 示例：国内固定 8 元，可根据实际业务添加更复杂的计算逻辑
        // 例如：根据重量、距离、是否包邮等条件计算
        return FIXED_SHIPPING_CENTS;
    }
}
