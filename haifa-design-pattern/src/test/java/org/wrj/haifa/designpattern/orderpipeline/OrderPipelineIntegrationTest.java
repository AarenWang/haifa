package org.wrj.haifa.designpattern.orderpipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderPipeline;
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
}
