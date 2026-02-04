package org.wrj.haifa.designpattern.orderpipeline.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;
import org.wrj.haifa.designpattern.orderpipeline.strategy.discount.DiscountNormal;
import org.wrj.haifa.designpattern.orderpipeline.strategy.discount.DiscountSVIP;
import org.wrj.haifa.designpattern.orderpipeline.strategy.discount.DiscountStrategy;
import org.wrj.haifa.designpattern.orderpipeline.strategy.discount.DiscountVIP;
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
        List<DiscountStrategy> strategies = Arrays.asList(
                new DiscountVIP(),
                new DiscountNormal(),
                new DiscountSVIP()
        );
        StrategyRegistry<DiscountStrategy> registry = new StrategyRegistry<>(strategies, "Discount");

        // When & Then
        assertTrue(registry.get("VIP").isPresent());
        assertTrue(registry.get("NORMAL").isPresent());
        assertTrue(registry.get("SVIP").isPresent());
        assertFalse(registry.get("UNKNOWN").isPresent());
    }

    @Test
    @DisplayName("测试策略注册表 - 获取所有键")
    void testGetAllKeys() {
        // Given
        List<DiscountStrategy> strategies = Arrays.asList(
                new DiscountVIP(),
                new DiscountNormal()
        );
        StrategyRegistry<DiscountStrategy> registry = new StrategyRegistry<>(strategies, "Discount");

        // When
        var keys = registry.getAllKeys();

        // Then
        assertEquals(2, keys.size());
        assertTrue(keys.contains("VIP"));
        assertTrue(keys.contains("NORMAL"));
    }

    @Test
    @DisplayName("测试折扣策略计算")
    void testDiscountStrategyCalculation() {
        // Given
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 10000);
        OrderContext ctx = new OrderContext(request);
        ctx.setBasePriceCents(10000);

        // When & Then - VIP 策略（5% 折扣）
        DiscountVIP vipStrategy = new DiscountVIP();
        assertEquals(500, vipStrategy.calcDiscountCents(ctx));

        // When & Then - SVIP 策略（10% 折扣）
        DiscountSVIP svipStrategy = new DiscountSVIP();
        assertEquals(1000, svipStrategy.calcDiscountCents(ctx));

        // When & Then - Normal 策略（无折扣）
        DiscountNormal normalStrategy = new DiscountNormal();
        assertEquals(0, normalStrategy.calcDiscountCents(ctx));
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
}
