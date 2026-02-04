package org.wrj.haifa.designpattern.orderpipeline.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 税费处理器 - 职责链中的税费计算环节
 * 
 * <p>根据国家计算税费（示例中只对中国订单收取 6% 的税）</p>
 * <p>实际业务中可以扩展为策略模式，支持不同国家/地区的税率</p>
 * 
 * @author wrj
 */
@Component
@Order(40) // 在运费之后执行
public class TaxHandler implements OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(TaxHandler.class);

    private static final double CN_TAX_RATE = 0.06; // 中国增值税率 6%

    @Override
    public void handle(OrderContext ctx) {
        String country = ctx.getRequest().getCountry();
        int taxCents = 0;

        if ("CN".equals(country)) {
            // 中国订单：对（基础价格 - 折扣）征收 6% 的税
            int taxableAmount = Math.max(0, ctx.getBasePriceCents() - ctx.getDiscountCents());
            taxCents = (int) Math.round(taxableAmount * CN_TAX_RATE);
        }
        // 其他国家暂不计税（实际业务中需要根据当地税法计算）

        ctx.setTaxCents(taxCents);

        log.info("TaxHandler: Country [{}] -> Tax {} cents (taxable base: {} cents)",
                country, taxCents, ctx.getBasePriceCents() - ctx.getDiscountCents());
    }
}
