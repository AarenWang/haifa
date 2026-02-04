package org.wrj.haifa.designpattern.orderpipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderPipeline;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;

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
    @DisplayName("测试中国 VIP 用户订单计算")
    void testChinaVIPOrder() {
        // Given: 中国 VIP 用户，订单金额 10000 分（100 元）
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 10000);

        // When: 执行订单处理管道
        OrderContext result = pipeline.execute(request);

        // Then: 验证计算结果
        // 基础价格：10000 分
        assertEquals(10000, result.getBasePriceCents());

        // VIP 折扣：10000 * 5% = 500 分
        assertEquals(500, result.getDiscountCents());

        // 中国运费：800 分（固定 8 元）
        assertEquals(800, result.getShippingCents());

        // 中国税费：(10000 - 500) * 6% = 570 分
        assertEquals(570, result.getTaxCents());

        // 应付金额：(10000 - 500) + 800 + 570 = 10870 分
        assertEquals(10870, result.getPayableCents());
    }

    @Test
    @DisplayName("测试中国普通用户订单计算")
    void testChinaNormalOrder() {
        // Given: 中国普通用户，订单金额 10000 分
        OrderRequest request = new OrderRequest("APP", "CN", "NORMAL", 10000);

        // When
        OrderContext result = pipeline.execute(request);

        // Then
        assertEquals(10000, result.getBasePriceCents());
        assertEquals(0, result.getDiscountCents()); // 普通用户无折扣
        assertEquals(800, result.getShippingCents());
        assertEquals(600, result.getTaxCents()); // 10000 * 6% = 600
        assertEquals(11400, result.getPayableCents()); // 10000 + 800 + 600
    }

    @Test
    @DisplayName("测试美国 VIP 用户订单计算")
    void testUSVIPOrder() {
        // Given: 美国 VIP 用户，订单金额 10000 分
        OrderRequest request = new OrderRequest("WEB", "US", "VIP", 10000);

        // When
        OrderContext result = pipeline.execute(request);

        // Then
        assertEquals(10000, result.getBasePriceCents());
        assertEquals(500, result.getDiscountCents()); // VIP 5% 折扣
        assertEquals(1500, result.getShippingCents()); // 美国运费 15 USD
        assertEquals(0, result.getTaxCents()); // 美国暂不计税
        assertEquals(11000, result.getPayableCents()); // (10000 - 500) + 1500
    }

    @Test
    @DisplayName("测试日本 SVIP 用户订单计算")
    void testJapanSVIPOrder() {
        // Given: 日本 SVIP 用户，订单金额 20000 分
        OrderRequest request = new OrderRequest("WEB", "JP", "SVIP", 20000);

        // When
        OrderContext result = pipeline.execute(request);

        // Then
        assertEquals(20000, result.getBasePriceCents());
        assertEquals(2000, result.getDiscountCents()); // SVIP 10% 折扣
        assertEquals(1200, result.getShippingCents()); // 日本运费 1200 JPY
        assertEquals(0, result.getTaxCents()); // 日本暂不计税
        assertEquals(19200, result.getPayableCents()); // (20000 - 2000) + 1200
    }

    @Test
    @DisplayName("测试小额订单计算")
    void testSmallOrder() {
        // Given: 中国 VIP 用户，订单金额 100 分（1 元）
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 100);

        // When
        OrderContext result = pipeline.execute(request);

        // Then
        assertEquals(100, result.getBasePriceCents());
        assertEquals(5, result.getDiscountCents()); // 100 * 5% = 5
        assertEquals(800, result.getShippingCents());
        assertEquals(6, result.getTaxCents()); // (100 - 5) * 6% ≈ 6
        assertEquals(901, result.getPayableCents()); // (100 - 5) + 800 + 6
    }
}
