package org.wrj.haifa.designpattern.orderpipeline.chain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 职责链模式单元测试
 * 
 * @author wrj
 */
class OrderPipelineTest {

    private List<String> executionOrder;

    @BeforeEach
    void setUp() {
        executionOrder = new ArrayList<>();
    }

    @Test
    @DisplayName("测试处理器按顺序执行")
    void testHandlersExecuteInOrder() {
        // Given
        List<OrderHandler> handlers = List.of(
                ctx -> executionOrder.add("first"),
                ctx -> executionOrder.add("second"),
                ctx -> executionOrder.add("third")
        );
        OrderPipeline pipeline = new OrderPipeline(handlers);
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 1000);

        // When
        pipeline.execute(request);

        // Then
        assertEquals(3, executionOrder.size());
        assertEquals("first", executionOrder.get(0));
        assertEquals("second", executionOrder.get(1));
        assertEquals("third", executionOrder.get(2));
    }

    @Test
    @DisplayName("测试处理器可以修改上下文")
    void testHandlersCanModifyContext() {
        // Given
        AtomicInteger counter = new AtomicInteger(0);
        List<OrderHandler> handlers = List.of(
                ctx -> ctx.setBasePriceCents(counter.incrementAndGet()),
                ctx -> ctx.setDiscountCents(counter.incrementAndGet()),
                ctx -> ctx.setShippingCents(counter.incrementAndGet())
        );
        OrderPipeline pipeline = new OrderPipeline(handlers);
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 1000);

        // When
        OrderContext result = pipeline.execute(request);

        // Then
        assertEquals(1, result.getBasePriceCents());
        assertEquals(2, result.getDiscountCents());
        assertEquals(3, result.getShippingCents());
    }

    @Test
    @DisplayName("测试空处理器列表")
    void testEmptyHandlerList() {
        // Given
        List<OrderHandler> handlers = List.of();
        OrderPipeline pipeline = new OrderPipeline(handlers);
        OrderRequest request = new OrderRequest("WEB", "CN", "VIP", 1000);

        // When
        OrderContext result = pipeline.execute(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getBasePriceCents());
        assertEquals(0, result.getPayableCents());
    }

    @Test
    @DisplayName("测试上下文正确传递请求信息")
    void testContextContainsRequest() {
        // Given
        OrderRequest request = new OrderRequest("APP", "US", "NORMAL", 5000);
        List<OrderHandler> handlers = List.of(
                ctx -> {
                    assertEquals("APP", ctx.getRequest().getChannel());
                    assertEquals("US", ctx.getRequest().getCountry());
                    assertEquals("NORMAL", ctx.getRequest().getUserTier());
                    assertEquals(5000, ctx.getRequest().getAmountCents());
                }
        );
        OrderPipeline pipeline = new OrderPipeline(handlers);

        // When & Then (assertions in handler)
        pipeline.execute(request);
    }
}
