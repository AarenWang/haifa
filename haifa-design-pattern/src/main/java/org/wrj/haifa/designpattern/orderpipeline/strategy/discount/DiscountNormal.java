package org.wrj.haifa.designpattern.orderpipeline.strategy.discount;

import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 普通用户折扣策略
 * 
 * <p>示例：普通用户无折扣</p>
 * 
 * @author wrj
 */
@Component
public class DiscountNormal implements DiscountStrategy {

    @Override
    public String key() {
        return "NORMAL";
    }

    @Override
    public int calcDiscountCents(OrderContext ctx) {
        // 普通用户无折扣
        return 0;
    }
}
