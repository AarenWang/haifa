package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountScope;

/**
 * 商品级折扣规则：只产出候选折扣。
 */
public interface ItemDiscountRule {

    String id();

    default String mutexGroup() {
        return null;
    }

    default int priority() {
        return 100;
    }

    default boolean stackable() {
        return true;
    }

    boolean supports(OrderContext ctx, LineItem item);

    int calcDiscountCents(OrderContext ctx, LineItem item);

    default DiscountCandidate evaluate(OrderContext ctx, LineItem item) {
        int discountCents = calcDiscountCents(ctx, item);
        DiscountEntry entry = createDiscountEntry(ctx, item, discountCents);
        return DiscountCandidate.builder()
                .id(id())
                .scope(DiscountScope.ITEM)
                .mutexGroup(mutexGroup())
                .priority(priority())
                .discountCents(discountCents)
                .stackable(stackable())
                .meta("sku", item.getSku())
                .meta("ruleName", getClass().getSimpleName())
                .entry(entry)
                .build();
    }

    default DiscountEntry createDiscountEntry(OrderContext ctx, LineItem item, int amountCents) {
        return DiscountEntry.builder()
                .amountCents(amountCents)
                .source("ITEM_" + id())
                .ruleName(getClass().getSimpleName())
                .sku(item.getSku())
                .build();
    }
}
