package org.wrj.haifa.designpattern.orderpipeline.chain;

import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;

/**
 * 订单处理器接口 - 职责链模式中的处理器抽象
 * 
 * <p>每个处理器负责处理订单的一个环节，如定价、折扣、运费、税费等</p>
 * 
 * @author wrj
 */
public interface OrderHandler {

    /**
     * 处理订单上下文
     * 
     * @param ctx 订单上下文，包含请求信息和计算过程中的各项数据
     */
    void handle(OrderContext ctx);
}
