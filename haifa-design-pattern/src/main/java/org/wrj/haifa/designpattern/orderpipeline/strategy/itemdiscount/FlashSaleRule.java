package org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 秒杀折扣规则
 * SKU 以 "FS-" 开头的商品参与秒杀，享受 8 折（20% off）
 */
@Component
@Order(10)
public class FlashSaleRule implements ItemDiscountRule {
    
    private static final Logger log = LoggerFactory.getLogger(FlashSaleRule.class);
    private static final String SKU_PREFIX = "FS-";
    private static final double DISCOUNT_RATE = 0.20; // 20% off
    
    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return item.getSku() != null && item.getSku().startsWith(SKU_PREFIX);
    }

    @Override
    public int calcDiscountCents(OrderContext ctx, LineItem item) {
        int lineRaw = item.getRawLineCents();
        int discount = (int) Math.round(lineRaw * DISCOUNT_RATE);
        log.debug("FlashSaleRule: SKU [{}] -> Discount {} cents ({}% off)", 
                item.getSku(), discount, (int)(DISCOUNT_RATE * 100));
        return discount;
    }

    @Override
    public String tag() {
        return "FLASH_SALE";
    }

    @Override
    public int priority() {
        return 10;
    }
}
