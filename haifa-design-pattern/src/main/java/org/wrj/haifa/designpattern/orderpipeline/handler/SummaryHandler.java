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
        // 计算小计（基础价格 - 折扣）
        int subtotal = ctx.getBasePriceCents() - ctx.getDiscountCents();

        // 计算最终应付金额 = 小计 + 运费 + 税费
        int payableCents = Math.max(0, subtotal) + ctx.getShippingCents() + ctx.getTaxCents();
        ctx.setPayableCents(payableCents);

        log.info("SummaryHandler: Calculation breakdown:");
        log.info("  Base price:    {} cents", ctx.getBasePriceCents());
        log.info("  - Discount:    {} cents", ctx.getDiscountCents());
        log.info("  = Subtotal:    {} cents", subtotal);
        log.info("  + Shipping:    {} cents", ctx.getShippingCents());
        log.info("  + Tax:         {} cents", ctx.getTaxCents());
        log.info("  -------------------------");
        log.info("  = Payable:     {} cents", payableCents);
    }
}
