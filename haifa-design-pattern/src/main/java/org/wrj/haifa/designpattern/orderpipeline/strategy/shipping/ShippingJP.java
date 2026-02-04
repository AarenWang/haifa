package org.wrj.haifa.designpattern.orderpipeline.strategy.shipping;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 日本运费策略
 * 
 * <p>示例：日本固定运费 1200 日元（1200 分/日元）</p>
 * 
 * @author wrj
 */
@Component
public class ShippingJP implements ShippingStrategy {

    private static final int FIXED_SHIPPING_CENTS = 1200; // 1200 JPY

    @Override
    public String key() {
        return "JP";
    }

    @Override
    public int calcShippingCents(OrderContext ctx) {
        // 示例：日本固定 1200 日元
        return FIXED_SHIPPING_CENTS;
    }
}
