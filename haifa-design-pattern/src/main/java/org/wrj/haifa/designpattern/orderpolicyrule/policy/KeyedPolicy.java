package org.wrj.haifa.designpattern.orderpolicyrule.policy;

/**
 * 带 key 的策略，用于注册表管理。
 */
public interface KeyedPolicy extends DiscountPolicy {

    String key();
}
