package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;

/**
 * 订单折扣分摊处理器
 *
 * <p>将订单级折扣按比例分摊到每个商品行，用于发票/退款/结算/税基计算</p>
 * <p>分摊策略：按各商品折后金额占比分摊，最后一行补差确保总额一致</p>
 *
 * <p>执行顺序：在 OrderDiscountHandler (@Order 30) 之后，ShippingHandler (@Order 40) 之前</p>
 *
 * @author wrj
 */
@Component
@Order(35)
public class OrderDiscountAllocationHandler implements OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderDiscountAllocationHandler.class);

    @Override
    public void handle(OrderContext ctx) {
        // 无商品明细或无订单折扣时跳过
        if (!ctx.getRequest().hasItems() || ctx.getOrderDiscountCents() <= 0) {
            return;
        }

        int orderDiscount = ctx.getOrderDiscountCents();
        int totalAfterItemDisc = ctx.getItemsAfterItemDiscCents();

        if (totalAfterItemDisc <= 0) {
            return;
        }

        log.info("OrderDiscountAllocationHandler: allocating {} cents across {} items",
                orderDiscount, ctx.getRequest().getItems().size());

        // 第一轮：按比例分摊
        int totalAllocated = 0;
        LineItem lastItem = null;

        for (LineItem item : ctx.getRequest().getItems()) {
            lastItem = item;
            int itemFinalCents = item.getFinalLineCents();

            // 按该行折后金额占比分摊订单折扣
            // 公式：allocated = orderDiscount * (itemFinalCents / totalAfterItemDisc)
            long allocatedLong = ((long) orderDiscount * itemFinalCents) / totalAfterItemDisc;
            int allocated = (int) allocatedLong;

            item.setAllocatedOrderDiscountCents(allocated);
            totalAllocated += allocated;

            log.debug("  Item {}: finalLineCents={}, allocated={} cents",
                    item.getSku(), itemFinalCents, allocated);
        }

        // 第二轮：最后一行补差（确保分摊总额 = 订单折扣总额）
        if (lastItem != null && totalAllocated != orderDiscount) {
            int diff = orderDiscount - totalAllocated;
            int lastItemOriginalAllocation = lastItem.getAllocatedOrderDiscountCents();
            lastItem.setAllocatedOrderDiscountCents(lastItemOriginalAllocation + diff);

            log.debug("  Adjusting last item {} by {} cents: {} -> {}",
                    lastItem.getSku(), diff, lastItemOriginalAllocation,
                    lastItem.getAllocatedOrderDiscountCents());
        }

        // 将订单折扣明细复制到各商品的 allocatedOrderDiscountEntries
        // 这样每个商品都能看到完整订单折扣信息
        for (LineItem item : ctx.getRequest().getItems()) {
            if (item.getAllocatedOrderDiscountCents() > 0) {
                // 为每个商品创建一份分摊的折扣明细
                for (DiscountEntry orderEntry : ctx.getOrderDiscountEntries()) {
                    DiscountEntry allocatedEntry = DiscountEntry.builder()
                            .amountCents(item.getAllocatedOrderDiscountCents())
                            .source(orderEntry.getSource() + "_ALLOCATED")
                            .ruleName(orderEntry.getRuleName())
                            .couponId(orderEntry.getCouponId())
                            .sku(item.getSku())
                            .remark("Allocated from order discount")
                            .build();

                    item.getAllocatedOrderDiscountEntries().add(allocatedEntry);
                }
            }
        }

        log.info("OrderDiscountAllocationHandler: total allocated = {} cents", totalAllocated);
    }
}
