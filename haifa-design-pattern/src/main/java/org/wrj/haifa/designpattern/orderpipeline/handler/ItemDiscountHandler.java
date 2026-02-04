package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount.ItemDiscountRule;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;

import java.util.List;

/**
 * 商品级折扣处理器：逐行计算商品折扣，支持叠加规则
 * 记录每笔折扣的明细信息用于审计
 */
@Component
@Order(20)
public class ItemDiscountHandler implements OrderHandler {

    private final List<ItemDiscountRule> rules;

    public ItemDiscountHandler(List<ItemDiscountRule> rules) {
        this.rules = rules;
    }

    @Override
    public void handle(OrderContext ctx) {
        int subtotal = 0;
        int afterItemDisc = 0;
        int totalItemDiscount = 0;

        if (!ctx.getRequest().hasItems()) {
            // 兼容老版：单一 amountCents
            int raw = ctx.getRequest().getAmountCents();
            subtotal = raw;
            ctx.setItemsSubtotalCents(subtotal);
            ctx.setItemsAfterItemDiscCents(raw);
            ctx.setItemDiscountCents(0);
            ctx.setBasePriceCents(raw);
            return;
        }

        for (LineItem item : ctx.getRequest().getItems()) {
            int lineRaw = item.getRawLineCents();
            subtotal += lineRaw;

            int disc = 0;
            for (ItemDiscountRule r : rules) {
                if (r.supports(ctx, item)) {
                    int d = r.calcDiscountCents(ctx, item);
                    if (d > 0) {
                        item.getDiscountTags().add(r.tag());

                        // 记录折扣明细
                        DiscountEntry entry = r.createDiscountEntry(ctx, item, d);
                        item.getItemDiscountEntries().add(entry);
                        ctx.addItemDiscountEntry(entry);

                        disc += d;
                    }
                }
            }

            int applied = Math.min(disc, lineRaw);
            item.setItemDiscountCents(applied);
            item.setFinalLineCents(lineRaw - applied);
            afterItemDisc += item.getFinalLineCents();
            totalItemDiscount += applied;
        }

        ctx.setItemsSubtotalCents(subtotal);
        ctx.setItemsAfterItemDiscCents(afterItemDisc);
        ctx.setItemDiscountCents(totalItemDiscount);
        ctx.setBasePriceCents(subtotal);
    }
}
