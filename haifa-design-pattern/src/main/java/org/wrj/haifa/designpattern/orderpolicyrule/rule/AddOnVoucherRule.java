package org.wrj.haifa.designpattern.orderpolicyrule.rule;

import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 加购赠券：买 2 件送 3 元（可叠加）。
 */
public class AddOnVoucherRule implements ItemDiscountRule {

    @Override
    public String id() {
        return "ADDON_VOUCHER";
    }

    @Override
    public String mutexGroup() {
        return "ADDON";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean stackable() {
        return true;
    }

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return item.getQty() >= 2;
    }

    @Override
    public int calcDiscountCents(OrderContext ctx, LineItem item) {
        return 300;
    }
}
