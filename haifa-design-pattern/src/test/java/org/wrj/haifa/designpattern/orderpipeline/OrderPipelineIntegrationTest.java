package org.wrj.haifa.designpattern.orderpipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderPipeline;
import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单处理管道集成测试
 *
 * <p>测试策略模式 + 职责链模式 + Spring IoC 的完整工作流程</p>
 * <p>使用纯 Spring Context 而非 Spring Boot Test 以避免版本兼容性问题</p>
 * <p>测试折扣明细记录与订单折扣分摊功能</p>
 *
 * @author wrj
 */
class OrderPipelineIntegrationTest {

    private OrderPipeline pipeline;

    @Configuration
    @ComponentScan(
        basePackages = {
            "org.wrj.haifa.designpattern.orderpipeline.chain",
            "org.wrj.haifa.designpattern.orderpipeline.handler",
            "org.wrj.haifa.designpattern.orderpipeline.strategy"
        }
    )
    static class TestConfig {
    }

    @BeforeEach
    void setUp() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
        pipeline = context.getBean(OrderPipeline.class);
    }

    @Test
    @DisplayName("中国 VIP 用户：商品折扣 + 优惠券")
    void testChinaVIPOrderWithItemsAndCoupon() {
        OrderRequest request = new OrderRequest();
        request.setChannel("WEB");
        request.setCountry("CN");
        request.setUserTier("VIP");
        request.setCouponCode("C100-20");
        request.setItems(List.of(
                new LineItem("FS-1001", 5000, 1),
                new LineItem("SKU-2002", 3000, 1),
                new LineItem("SKU-2003", 2000, 2)
        ));

        OrderContext result = pipeline.execute(request);

        assertEquals(12000, result.getBasePriceCents());
        assertEquals(12000, result.getItemsSubtotalCents());
        assertEquals(1600, result.getItemDiscountCents());
        assertEquals(10400, result.getItemsAfterItemDiscCents());
        assertEquals(2000, result.getOrderDiscountCents());
        assertEquals(3600, result.getDiscountCents());
        assertEquals(800, result.getShippingCents());
        assertEquals(504, result.getTaxCents());
        assertEquals(9704, result.getPayableCents());

        // 验证折扣明细
        assertFalse(result.getDiscountEntries().isEmpty(), "应该有折扣明细记录");
        assertFalse(result.getItemDiscountEntries().isEmpty(), "应该有商品级折扣明细");
        assertFalse(result.getOrderDiscountEntries().isEmpty(), "应该有订单级折扣明细");

        // 验证订单折扣分摊
        List<LineItem> items = result.getRequest().getItems();
        int totalAllocated = items.stream()
                .mapToInt(LineItem::getAllocatedOrderDiscountCents)
                .sum();
        assertEquals(2000, totalAllocated, "分摊总额应等于订单折扣");
    }

    @Test
    @DisplayName("美国订单：10% 优惠码")
    void testUSOrderWithPromoCode() {
        OrderRequest request = new OrderRequest();
        request.setChannel("APP");
        request.setCountry("US");
        request.setUserTier("NORMAL");
        request.setCouponCode("OFF10");
        request.setItems(List.of(
                new LineItem("SKU-1100", 4000, 1),
                new LineItem("SKU-2200", 6000, 1)
        ));

        OrderContext result = pipeline.execute(request);

        assertEquals(10000, result.getItemsSubtotalCents());
        assertEquals(0, result.getItemDiscountCents());
        assertEquals(10000, result.getItemsAfterItemDiscCents());
        assertEquals(1000, result.getOrderDiscountCents());
        assertEquals(1000, result.getDiscountCents());
        assertEquals(1500, result.getShippingCents());
        assertEquals(0, result.getTaxCents());
        assertEquals(10500, result.getPayableCents());

        // 验证折扣分摊：1000 分到 4000 和 6000 的商品上
        // 4000/10000 * 1000 = 400, 6000/10000 * 1000 = 600
        List<LineItem> items = result.getRequest().getItems();
        assertEquals(400, items.get(0).getAllocatedOrderDiscountCents(),
                "第一个商品应分摊 400 分折扣");
        assertEquals(600, items.get(1).getAllocatedOrderDiscountCents(),
                "第二个商品应分摊 600 分折扣");

        // 验证折扣明细
        assertFalse(result.getOrderDiscountEntries().isEmpty(), "应该有订单级折扣明细");
        assertEquals(1, result.getOrderDiscountEntries().size(), "应该有一条订单折扣记录");
        assertEquals("ORDER_DISCOUNT", result.getOrderDiscountEntries().get(0).getSource());
        assertEquals("OFF10", result.getOrderDiscountEntries().get(0).getCouponId());
    }

    @Test
    @DisplayName("兼容旧版：无商品列表时仍可计算")
    void testLegacySingleAmountOrder() {
        OrderRequest request = new OrderRequest("APP", "CN", "NORMAL", 10000);

        OrderContext result = pipeline.execute(request);

        assertEquals(10000, result.getBasePriceCents());
        assertEquals(10000, result.getItemsSubtotalCents());
        assertEquals(0, result.getItemDiscountCents());
        assertEquals(10000, result.getItemsAfterItemDiscCents());
        assertEquals(0, result.getOrderDiscountCents());
        assertEquals(0, result.getDiscountCents());
        assertEquals(800, result.getShippingCents());
        assertEquals(600, result.getTaxCents());
        assertEquals(11400, result.getPayableCents());
    }

    @Test
    @DisplayName("折扣明细记录验证")
    void testDiscountEntryDetails() {
        OrderRequest request = new OrderRequest();
        request.setChannel("WEB");
        request.setCountry("CN");
        request.setUserTier("VIP");
        request.setCouponCode("C100-20");
        request.setItems(List.of(
                new LineItem("FS-1001", 5000, 1),  // 秒杀品 8 折
                new LineItem("SKU-2002", 3000, 1)   // 普通 VIP 95 折
        ));

        OrderContext result = pipeline.execute(request);

        // 验证商品级折扣明细
        List<DiscountEntry> itemEntries = result.getItemDiscountEntries();
        assertFalse(itemEntries.isEmpty(), "应该有商品级折扣明细");

        // 验证秒杀折扣
        boolean hasFlashSaleDiscount = itemEntries.stream()
                .anyMatch(e -> e.getSource().equals("ITEM_FLASH_SALE") && e.getSku().equals("FS-1001"));
        assertTrue(hasFlashSaleDiscount, "应该有秒杀折扣记录");

        // 验证 VIP 折扣
        boolean hasVIPDiscount = itemEntries.stream()
                .anyMatch(e -> e.getSource().equals("ITEM_VIP") && e.getAmountCents() > 0);
        assertTrue(hasVIPDiscount, "应该有 VIP 折扣记录");

        // 验证订单级折扣明细
        List<DiscountEntry> orderEntries = result.getOrderDiscountEntries();
        assertEquals(1, orderEntries.size(), "应该有一条订单折扣记录");
        assertEquals("C100-20", orderEntries.get(0).getCouponId(), "应该记录优惠券码");
    }

    @Test
    @DisplayName("订单折扣分摊-最后一行补差验证")
    void testOrderDiscountAllocationWithRoundingAdjustment() {
        OrderRequest request = new OrderRequest();
        request.setChannel("WEB");
        request.setCountry("CN");
        request.setUserTier("NORMAL");
        request.setCouponCode("C100-20");  // 满 100 减 20
        request.setItems(List.of(
                new LineItem("SKU-1001", 3000, 1),  // 30 元
                new LineItem("SKU-1002", 3000, 1),  // 30 元
                new LineItem("SKU-1003", 4000, 1)   // 40 元
        ));  // 总计 100 元，减 20

        OrderContext result = pipeline.execute(request);

        assertEquals(2000, result.getOrderDiscountCents());

        List<LineItem> items = result.getRequest().getItems();
        int totalAllocated = items.stream()
                .mapToInt(LineItem::getAllocatedOrderDiscountCents)
                .sum();

        // 验证分摊总额精确等于订单折扣
        assertEquals(2000, totalAllocated, "分摊总额应精确等于订单折扣（含补差）");

        // 验证每个商品都有非负的分摊金额
        for (LineItem item : items) {
            assertTrue(item.getAllocatedOrderDiscountCents() >= 0,
                    "每个商品的分摊折扣应为非负数: " + item.getSku());
        }
    }

    @Test
    @DisplayName("商品行总折扣和最终应付金额验证")
    void testLineItemTotalDiscountAndFinalPayable() {
        OrderRequest request = new OrderRequest();
        request.setChannel("WEB");
        request.setCountry("CN");
        request.setUserTier("VIP");
        request.setCouponCode("C100-20");
        request.setItems(List.of(
                new LineItem("FS-1001", 5000, 1)  // 秒杀 8 折 = 1000 折扣
        ));

        OrderContext result = pipeline.execute(request);

        List<LineItem> items = result.getRequest().getItems();
        LineItem item = items.get(0);

        // 商品级折扣：5000 * 0.2 = 1000
        assertEquals(1000, item.getItemDiscountCents(), "商品折扣应为 1000 分");

        // 订单折扣分摊：2000 * (4000 / 10400) ≈ 769
        int allocatedOrderDiscount = item.getAllocatedOrderDiscountCents();
        assertTrue(allocatedOrderDiscount > 0, "应有订单折扣分摊");

        // 总折扣 = 商品折扣 + 分摊的订单折扣
        assertEquals(item.getItemDiscountCents() + allocatedOrderDiscount,
                item.getTotalDiscountCents(), "总折扣应为商品折扣加订单折扣分摊");

        // 最终应付行金额
        assertEquals(5000 - item.getItemDiscountCents() - allocatedOrderDiscount,
                item.getFinalPayableLineCents(), "最终应付行金额计算正确");
    }
}
