package org.wrj.haifa.designpattern.orderpipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单处理管道示例应用 - 演示策略模式 + 职责链模式 + Spring IoC 的混合使用
 * 
 * <h2>设计模式说明</h2>
 * 
 * <h3>1. 职责链模式 (Chain of Responsibility)</h3>
 * <ul>
 *   <li>一串 {@link org.wrj.haifa.designpattern.orderpipeline.chain.OrderHandler}，每个 handler 做一件事</li>
 *   <li>按顺序处理 {@link org.wrj.haifa.designpattern.orderpipeline.model.OrderContext}</li>
 *   <li>通过 {@link org.springframework.core.annotation.Order} 注解控制执行顺序</li>
 * </ul>
 * 
 * <h3>2. 策略模式 (Strategy)</h3>
 * <ul>
 *   <li>某个 handler 内部根据条件选择一个策略实现</li>
 *   <li>例如：运费策略（国内/国际）、折扣策略（VIP/普通）等</li>
 *   <li>使用 {@link org.wrj.haifa.designpattern.orderpipeline.strategy.StrategyRegistry} 管理策略</li>
 * </ul>
 * 
 * <h3>3. Spring IoC 集成</h3>
 * <ul>
 *   <li>所有 handler / strategy 都是 {@code @Component}，自动注入</li>
 *   <li>用 {@code @Order} 控制链顺序</li>
 *   <li>用"注册表（Registry）"方式把策略按 key 管理起来</li>
 * </ul>
 * 
 * <h2>处理链顺序</h2>
 * <ol>
 *   <li>BasePriceHandler (Order=10) - 设置基础价格</li>
 *   <li>DiscountHandler (Order=20) - 计算折扣（使用策略模式）</li>
 *   <li>ShippingHandler (Order=30) - 计算运费（使用策略模式）</li>
 *   <li>TaxHandler (Order=40) - 计算税费</li>
 *   <li>SummaryHandler (Order=50) - 汇总计算应付金额</li>
 * </ol>
 * 
 * <h2>扩展指南</h2>
 * <ul>
 *   <li>添加新策略：创建新的 {@code @Component} 实现对应策略接口，无需修改其他代码</li>
 *   <li>添加新处理器：创建新的 {@code @Component} 实现 OrderHandler，用 @Order 指定顺序</li>
 *   <li>动态启停处理器：给 handler 添加 supports(ctx) 方法进行过滤</li>
 * </ul>
 * 
 * @author wrj
 * @see org.wrj.haifa.designpattern.orderpipeline.chain.OrderPipeline
 * @see org.wrj.haifa.designpattern.orderpipeline.strategy.StrategyRegistry
 */
@SpringBootApplication
public class OrderPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPipelineApplication.class, args);
    }
}
