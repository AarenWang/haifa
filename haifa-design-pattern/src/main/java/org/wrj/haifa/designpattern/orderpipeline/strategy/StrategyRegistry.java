package org.wrj.haifa.designpattern.orderpipeline.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 策略注册表 - 通用的策略管理器
 * 
 * <p>负责收集所有 {@link KeyedStrategy} 的实现，并按 key 组织成 Map 供快速查找</p>
 * <p>这是一个泛型类，可以复用于不同类型的策略接口</p>
 * 
 * <p>使用方式：为每种策略类型创建一个具体的 Registry Bean，参见
 * {@link org.wrj.haifa.designpattern.orderpipeline.strategy.shipping.ShippingStrategyRegistry}</p>
 * 
 * @param <T> 策略类型，必须实现 {@link KeyedStrategy} 接口
 * @author wrj
 */
public class StrategyRegistry<T extends KeyedStrategy> {

    private static final Logger log = LoggerFactory.getLogger(StrategyRegistry.class);

    private final Map<String, T> strategyMap;
    private final String strategyTypeName;

    /**
     * 构造策略注册表
     * 
     * @param strategies 所有策略实现的列表（由 Spring 注入）
     * @param strategyTypeName 策略类型名称，用于日志输出
     */
    public StrategyRegistry(List<T> strategies, String strategyTypeName) {
        this.strategyTypeName = strategyTypeName;
        this.strategyMap = strategies.stream().collect(Collectors.toMap(
                KeyedStrategy::key,
                Function.identity(),
                (existing, replacement) -> {
                    throw new IllegalStateException(
                            String.format("Duplicate %s strategy key: %s", strategyTypeName, existing.key()));
                }
        ));

        log.info("{} Registry initialized with {} strategies: {}",
                strategyTypeName, strategyMap.size(), strategyMap.keySet());
    }

    /**
     * 获取指定 key 的策略，如果不存在则抛出异常
     * 
     * @param key 策略键
     * @return 对应的策略实现
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public T getRequired(String key) {
        T strategy = strategyMap.get(key);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    String.format("No %s strategy found for key: %s. Available keys: %s",
                            strategyTypeName, key, strategyMap.keySet()));
        }
        return strategy;
    }

    /**
     * 获取指定 key 的策略（可选）
     * 
     * @param key 策略键
     * @return 包含策略的 Optional，如果不存在则返回空 Optional
     */
    public Optional<T> get(String key) {
        return Optional.ofNullable(strategyMap.get(key));
    }

    /**
     * 检查是否存在指定 key 的策略
     * 
     * @param key 策略键
     * @return 如果存在返回 true，否则返回 false
     */
    public boolean contains(String key) {
        return strategyMap.containsKey(key);
    }

    /**
     * 获取所有已注册的策略键
     * 
     * @return 策略键集合
     */
    public java.util.Set<String> getAllKeys() {
        return strategyMap.keySet();
    }
}
