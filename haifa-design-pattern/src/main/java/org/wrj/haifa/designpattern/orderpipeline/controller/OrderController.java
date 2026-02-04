package org.wrj.haifa.designpattern.orderpipeline.controller;

import org.springframework.web.bind.annotation.*;
import org.wrj.haifa.designpattern.orderpipeline.chain.OrderPipeline;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;

/**
 * 订单报价 REST 接口
 * 
 * <p>提供 HTTP 接口供客户端调用，演示策略模式 + 职责链模式的实际使用</p>
 * 
 * @author wrj
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderPipeline pipeline;

    public OrderController(OrderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 订单报价接口
     * 
     * <p>示例请求：</p>
     * <pre>
     * POST /order/quote
     * Content-Type: application/json
     * 
    * {
    *   "channel": "WEB",
    *   "country": "CN",
    *   "userTier": "VIP",
    *   "couponCode": "C100-20",
    *   "items": [
    *     { "sku": "FS-1001", "unitPriceCents": 5000, "qty": 1 },
    *     { "sku": "SKU-2002", "unitPriceCents": 3000, "qty": 1 }
    *   ]
    * }
    *
    * <p>amountCents 依旧支持，作为无商品明细时的兼容输入。</p>
     * </pre>
     * 
     * @param request 订单请求
     * @return 计算完成的订单上下文（包含各项费用明细）
     */
    @PostMapping("/quote")
    public OrderContext quote(@RequestBody OrderRequest request) {
        return pipeline.execute(request);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
