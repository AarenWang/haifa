package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 秒杀折扣：SKU 前缀为 FS- 的商品享受 8 折（20% off）。
 */
public class FlashSaleRule implements ItemDiscountRule {

    private static final String SKU_PREFIX = "FS-";
    private static final double DISCOUNT_RATE = 0.20;

    @Override
    public String id() {
        return "FLASH_SALE";
    }

    @Override
    public String mutexGroup() {
        return "PRICE_PROMO";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean stackable() {
        return false;
    }

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return item.getSku() != null && item.getSku().startsWith(SKU_PREFIX);
    }

    @Override
    public int calcDiscountCents(OrderContext ctx, LineItem item) {
        int lineRaw = item.getRawLineCents();
        return (int) Math.round(lineRaw * DISCOUNT_RATE);
    }
}
