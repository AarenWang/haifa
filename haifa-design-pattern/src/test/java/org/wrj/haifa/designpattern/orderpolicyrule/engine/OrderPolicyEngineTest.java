package org.wrj.haifa.designpattern.orderpolicyrule.engine;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.MutexByGroupTakeMaxPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.TakeMaxPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.AddOnVoucherRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.Coupon100Minus20Rule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.FlashSaleRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.ItemDiscountRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.OrderDiscountRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.PromoCode10OffRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.VipPriceRule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderPolicyEngineTest {

    @Test
    void engineAppliesItemAndOrderPolicies() {
        OrderRequest request = new OrderRequest();
        request.setUserTier("VIP");
        request.setCouponCode("C100-20");
        request.setItems(List.of(new LineItem("FS-1001", 20000, 1)));

        OrderContext ctx = new OrderContext(request);

        List<ItemDiscountRule> itemRules = List.of(
                new FlashSaleRule(),
                new VipPriceRule(),
                new AddOnVoucherRule()
        );
        List<OrderDiscountRule> orderRules = List.of(
                new Coupon100Minus20Rule(),
                new PromoCode10OffRule()
        );

        OrderPolicyEngine engine = new OrderPolicyEngine(
                itemRules,
                orderRules,
                new MutexByGroupTakeMaxPolicy(),
                new TakeMaxPolicy()
        );

        PolicyExecutionResult result = engine.execute(ctx);

        assertEquals(20000, ctx.getItemsSubtotalCents());
        assertEquals(4000, ctx.getItemDiscountCents());
        assertEquals(16000, ctx.getItemsAfterItemDiscCents());
        assertEquals(2000, ctx.getOrderDiscountCents());
        assertEquals(6000, ctx.getDiscountCents());
        assertEquals(1, result.getItemDecisions().size());
        assertFalse(ctx.getItemDiscountEntries().isEmpty());
        assertFalse(ctx.getOrderDiscountEntries().isEmpty());
    }
}
