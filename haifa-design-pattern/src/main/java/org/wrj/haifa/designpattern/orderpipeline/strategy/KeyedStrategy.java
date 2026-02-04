package org.wrj.haifa.designpattern.orderpipeline.strategy;

/**
 * 带键的策略接口 - 策略模式的基础抽象
 * 
 * <p>每个策略实现类都需要提供一个唯一的 key，用于策略注册表中的查找</p>
 * <p>例如：运费策略按国家区分 key 为 "CN"、"US"；折扣策略按用户等级区分 key 为 "VIP"、"NORMAL"</p>
 * 
 * @author wrj
 */
public interface KeyedStrategy {

    /**
     * 获取策略的唯一标识键
     * 
     * @return 策略键，如 "CN", "US", "VIP", "NORMAL" 等
     */
    String key();
}
