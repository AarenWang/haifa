package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 基础定价处理器 - 职责链中的第一个环节
 * 
 * <p>负责设置订单的基础价格，通常直接使用请求中的金额</p>
 * <p>实际业务中可能涉及：商品查询、汇率转换、价格策略等</p>
 * 
 * @author wrj
 */
@Component
@Order(10) // 第一个执行
public class BasePriceHandler implements OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(BasePriceHandler.class);

    @Override
    public void handle(OrderContext ctx) {
        int basePrice;
        if (ctx.getRequest().hasItems()) {
            int subtotal = 0;
            for (LineItem item : ctx.getRequest().getItems()) {
                subtotal += item.getRawLineCents();
            }
            basePrice = subtotal;
            ctx.setItemsSubtotalCents(subtotal);
        } else {
            basePrice = ctx.getRequest().getAmountCents();
            ctx.setItemsSubtotalCents(basePrice);
        }

        ctx.setBasePriceCents(basePrice);
        ctx.setDiscountCents(0); // 初始化折扣，兼容旧版字段

        log.info("BasePriceHandler: Set base price to {} cents", ctx.getBasePriceCents());
    }
}
