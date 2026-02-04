package org.wrj.haifa.designpattern.orderpipeline.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;
import org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount.DiscountVIPRule;
import org.wrj.haifa.designpattern.orderpipeline.strategy.itemdiscount.FlashSaleRule;
import org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount.Coupon100Minus20;
import org.wrj.haifa.designpattern.orderpipeline.strategy.orderdiscount.PromoCode10Off;
import org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingCN;
import org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingJP;
import org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingStrategy;
import org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingUS;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略注册表单元测试
 * 
 * @author wrj
 */
class StrategyRegistryTest {

    @Test
    @DisplayName("测试策略注册表 - 正常获取策略")
    void testGetRequired() {
        // Given
        List<ShippingStrategy> strategies = Arrays.asList(
                new ShippingCN(),
                new ShippingUS(),
                new ShippingJP()
        );
        StrategyRegistry<ShippingStrategy> registry = new StrategyRegistry<>(strategies, "Shipping");

        // When & Then
        assertNotNull(registry.getRequired("CN"));
        assertNotNull(registry.getRequired("US"));
        assertNotNull(registry.getRequired("JP"));
        assertEquals("CN", registry.getRequired("CN").key());
    }

    @Test
    @DisplayName("测试策略注册表 - 策略不存在时抛出异常")
    void testGetRequiredNotFound() {
        // Given
        List<ShippingStrategy> strategies = Arrays.asList(new ShippingCN());
        StrategyRegistry<ShippingStrategy> registry = new StrategyRegistry<>(strategies, "Shipping");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> registry.getRequired("US"));
    }

    @Test
    @DisplayName("测试策略注册表 - Optional 方式获取")
    void testGetOptional() {
        // Given
        List<ShippingStrategy> strategies = Arrays.asList(
            new ShippingCN(),
            new ShippingUS()
        );
        StrategyRegistry<ShippingStrategy> registry = new StrategyRegistry<>(strategies, "Shipping");

        assertTrue(registry.get("CN").isPresent());
        assertTrue(registry.get("US").isPresent());
        assertFalse(registry.get("JP").isPresent());
    }

    @Test
    @DisplayName("测试策略注册表 - 获取所有键")
    void testGetAllKeys() {
        // Given
        List<ShippingStrategy> strategies = Arrays.asList(
            new ShippingCN(),
            new ShippingUS()
        );
        StrategyRegistry<ShippingStrategy> registry = new StrategyRegistry<>(strategies, "Shipping");

        // When
        var keys = registry.getAllKeys();

        // Then
        assertEquals(2, keys.size());
        assertTrue(keys.contains("CN"));
        assertTrue(keys.contains("US"));
    }

    @Test
        @DisplayName("测试商品级折扣规则")
        void testItemDiscountRuleCalculation() {
        OrderRequest request = new OrderRequest();
        request.setChannel("WEB");
        request.setCountry("CN");
        request.setUserTier("VIP");
        LineItem flashSaleItem = new LineItem("FS-9001", 1000, 1);
        request.setItems(List.of(flashSaleItem));
        OrderContext ctx = new OrderContext(request);

        FlashSaleRule flashSaleRule = new FlashSaleRule();
        assertTrue(flashSaleRule.supports(ctx, flashSaleItem));
        assertEquals(200, flashSaleRule.calcDiscountCents(ctx, flashSaleItem));

        DiscountVIPRule vipRule = new DiscountVIPRule();
        assertTrue(vipRule.supports(ctx, flashSaleItem));
        assertEquals(50, vipRule.calcDiscountCents(ctx, flashSaleItem));
    }

    @Test
    @DisplayName("测试运费策略计算")
    void testShippingStrategyCalculation() {
        // Given
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 10000);
        OrderContext ctx = new OrderContext(request);

        // When & Then - 中国运费
        ShippingCN cnStrategy = new ShippingCN();
        assertEquals(800, cnStrategy.calcShippingCents(ctx));

        // When & Then - 美国运费
        ShippingUS usStrategy = new ShippingUS();
        assertEquals(1500, usStrategy.calcShippingCents(ctx));

        // When & Then - 日本运费
        ShippingJP jpStrategy = new ShippingJP();
        assertEquals(1200, jpStrategy.calcShippingCents(ctx));
    }

    @Test
    @DisplayName("测试订单级折扣策略")
    void testOrderDiscountStrategyCalculation() {
        OrderRequest request = new OrderRequest();
        request.setItems(List.of());
        OrderContext ctx = new OrderContext(request);
        ctx.setItemsAfterItemDiscCents(12000);
        request.setCouponCode("C100-20");

        Coupon100Minus20 coupon = new Coupon100Minus20();
        assertTrue(coupon.supports(ctx));
        assertEquals(2000, coupon.calcOrderDiscountCents(ctx));

        ctx.setItemsAfterItemDiscCents(15000);
        request.setCouponCode("OFF10");
        PromoCode10Off promo = new PromoCode10Off();
        assertTrue(promo.supports(ctx));
        assertEquals(1500, promo.calcOrderDiscountCents(ctx));
    }
}
