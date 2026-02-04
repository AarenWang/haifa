package org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount;

import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 商品折扣规则接口
 * 商品折扣不是"选一个"，而是"跑一组可叠加规则"
 * 支持 flashSale + 会员价叠加/互斥/取最大等逻辑
 */
public interface ItemDiscountRule {

    /**
     * 判断该规则是否适用于指定商品
     */
    boolean supports(OrderContext ctx, LineItem item);

    /**
     * 计算折扣金额（分）
     */
    int calcDiscountCents(OrderContext ctx, LineItem item);

    /**
     * 折扣标签，用于记录折扣来源
     */
    String tag();

    /**
     * 优先级（越小越先执行）
     */
    default int priority() {
        return 100;
    }

    /**
     * 创建折扣明细（默认实现）
     * 子类可以重写此方法提供更详细的明细信息
     */
    default DiscountEntry createDiscountEntry(OrderContext ctx, LineItem item, int amountCents) {
        return DiscountEntry.builder()
                .amountCents(amountCents)
                .source("ITEM_" + tag())
                .ruleName(this.getClass().getSimpleName())
                .sku(item.getSku())
                .build();
    }
}
