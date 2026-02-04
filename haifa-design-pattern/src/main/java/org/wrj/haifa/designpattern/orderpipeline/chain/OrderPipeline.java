package org.wrj.haifa.designpattern.orderpipeline.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;

import java.util.List;

/**
 * 订单处理管道 - 职责链模式的执行器
 * 
 * <p>负责按顺序执行所有注册的 {@link OrderHandler}，Spring 会按 @Order 注解排序注入</p>
 * 
 * @author wrj
 */
@Component
public class OrderPipeline {

    private static final Logger log = LoggerFactory.getLogger(OrderPipeline.class);

    private final List<OrderHandler> handlers;

    /**
     * 构造函数注入所有 OrderHandler 实现
     * Spring 会自动按照 @Order 注解的值进行排序
     * 
     * @param handlers 所有注册的处理器列表
     */
    public OrderPipeline(List<OrderHandler> handlers) {
        this.handlers = handlers;
        log.info("OrderPipeline initialized with {} handlers", handlers.size());
        handlers.forEach(h -> log.info("  -> {}", h.getClass().getSimpleName()));
    }

    /**
     * 执行订单处理管道
     * 
     * @param request 订单请求
     * @return 处理完成的订单上下文
     */
    public OrderContext execute(OrderRequest request) {
        log.info("Starting order pipeline for request: {}", request);

        OrderContext ctx = new OrderContext(request);

        for (OrderHandler handler : handlers) {
            log.debug("Executing handler: {}", handler.getClass().getSimpleName());
            long start = System.currentTimeMillis();

            handler.handle(ctx);

            long elapsed = System.currentTimeMillis() - start;
            log.debug("Handler {} completed in {}ms", handler.getClass().getSimpleName(), elapsed);
        }

        log.info("Order pipeline completed. Result: {}", ctx);
        return ctx;
    }
}
