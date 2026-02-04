package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 汇总处理器 - 职责链中的最后一个环节
 * 
 * <p>汇总所有费用，计算最终应付金额</p>
 * <p>公式：应付金额 = 基础价格 - 折扣 + 运费 + 税费</p>
 * 
 * @author wrj
 */
@Component
@Order(50) // 最后执行
public class SummaryHandler implements OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(SummaryHandler.class);

    @Override
    public void handle(OrderContext ctx) {
        // 支持两层折扣：先使用商品折后小计，再减订单折扣
        int afterItemDisc = ctx.getItemsAfterItemDiscCents() > 0 ? ctx.getItemsAfterItemDiscCents() : ctx.getBasePriceCents();
        int afterOrderDisc = Math.max(0, afterItemDisc - ctx.getOrderDiscountCents());

        int payableCents = afterOrderDisc + ctx.getShippingCents() + ctx.getTaxCents();
        ctx.setPayableCents(payableCents);

        log.info("SummaryHandler: Calculation breakdown:");
        log.info("  Items subtotal: {} cents", ctx.getItemsSubtotalCents());
        log.info("  - Item discount total: {} cents", ctx.getItemDiscountCents());
        log.info("  = After item discounts: {} cents", afterItemDisc);
        log.info("  - Order discount: {} cents", ctx.getOrderDiscountCents());
        log.info("  = After order discounts: {} cents", afterOrderDisc);
        log.info("  + Shipping:    {} cents", ctx.getShippingCents());
        log.info("  + Tax:         {} cents", ctx.getTaxCents());
        log.info("  -------------------------");
        log.info("  = Payable:     {} cents", payableCents);
    }
}
