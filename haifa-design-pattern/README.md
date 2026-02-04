# haifa-design-pattern

设计模式学习与实践模块，包含多种经典设计模式的 Java 实现示例。

---

## 订单处理管道（Order Pipeline）

> **职责链模式 + 策略模式 + Spring IoC 混合设计模式示例**

这是一个电商订单计价系统的完整实现，演示如何将多种设计模式与 Spring 框架优雅结合，构建可扩展、可测试的业务流水线。

### 📋 业务场景

订单从创建到最终计算应付金额，需要经过多个处理环节：

```
订单请求 → 基础定价 → 折扣计算 → 运费计算 → 税费计算 → 汇总结算
```

每个环节的具体策略可能因用户等级、配送地区等因素而不同。

### 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                        OrderPipeline                            │
│                     (Chain of Responsibility)                   │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│  │BasePrice│ → │Discount │ → │Shipping │ → │   Tax   │ → │ Summary │
│  │ Handler │   │ Handler │   │ Handler │   │ Handler │   │ Handler │
│  └─────────┘   └────┬────┘   └────┬────┘   └─────────┘   └─────────┘
│                     │             │
│              ┌──────┴──────┐ ┌────┴────┐
│              │DiscountReg. │ │ShipReg. │  (Strategy Registries)
│              └──────┬──────┘ └────┬────┘
│         ┌───────────┼───────────┐ │
│         ▼           ▼           ▼ │
│    ┌────────┐  ┌────────┐  ┌────────┐
│    │  VIP   │  │ NORMAL │  │  SVIP  │   ← Discount Strategies
│    │  5%    │  │   0%   │  │  10%   │
│    └────────┘  └────────┘  └────────┘
│                               │
│              ┌────────────────┼────────────────┐
│              ▼                ▼                ▼
│         ┌────────┐       ┌────────┐       ┌────────┐
│         │   CN   │       │   US   │       │   JP   │  ← Shipping Strategies
│         │  ¥8.00 │       │ $15.00 │       │ ¥12.00 │
│         └────────┘       └────────┘       └────────┘
└─────────────────────────────────────────────────────────────────┘
```

### 🎯 设计模式应用

#### 1. 职责链模式（Chain of Responsibility）

每个处理器实现 `OrderHandler` 接口，Spring 通过 `@Order` 注解自动排序并注入到 `OrderPipeline`：

```java
public interface OrderHandler {
    void handle(OrderContext ctx);
}

@Component
@Order(10)  // 执行优先级
public class BasePriceHandler implements OrderHandler {
    @Override
    public void handle(OrderContext ctx) {
        ctx.setBasePriceCents(ctx.getRequest().getAmountCents());
    }
}
```

**处理器执行顺序：**

| Order | Handler | 职责 |
|-------|---------|------|
| 10 | BasePriceHandler | 设置基础价格 |
| 20 | DiscountHandler | 根据用户等级计算折扣 |
| 30 | ShippingHandler | 根据配送地区计算运费 |
| 40 | TaxHandler | 根据地区计算税费 |
| 50 | SummaryHandler | 汇总计算最终应付金额 |

#### 2. 策略模式（Strategy Pattern）

通过 `KeyedStrategy<K>` 接口定义带标识的策略：

```java
public interface KeyedStrategy<K> {
    K key();  // 策略标识（如国家代码、用户等级）
}

public interface ShippingStrategy extends KeyedStrategy<String> {
    int calculateShipping(OrderContext ctx);
}

@Component
public class ShippingCN implements ShippingStrategy {
    @Override
    public String key() { return "CN"; }
    
    @Override
    public int calculateShipping(OrderContext ctx) {
        return 800;  // 中国境内运费 8 元
    }
}
```

#### 3. Spring IoC 自动装配

`StrategyRegistry<K, S>` 通过构造器注入自动收集所有策略实现：

```java
public class StrategyRegistry<K, S extends KeyedStrategy<K>> {
    private final Map<K, S> strategies;
    
    public StrategyRegistry(List<S> strategyList, String name) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(KeyedStrategy::key, s -> s));
    }
    
    public S getRequired(K key) {
        return Optional.ofNullable(strategies.get(key))
            .orElseThrow(() -> new IllegalArgumentException("Unknown key: " + key));
    }
}
```

### 📁 代码结构

```
src/main/java/org/wrj/haifa/designpattern/orderpipeline/
├── model/
│   ├── OrderRequest.java       # 订单请求（输入）
│   └── OrderContext.java       # 处理上下文（贯穿整个链）
├── chain/
│   ├── OrderHandler.java       # 处理器接口
│   └── OrderPipeline.java      # 职责链执行器
├── strategy/
│   ├── KeyedStrategy.java      # 带标识的策略接口
│   ├── StrategyRegistry.java   # 通用策略注册表
│   ├── discount/               # 折扣策略
│   │   ├── DiscountStrategy.java
│   │   ├── DiscountVIP.java        # VIP 折扣 5%
│   │   ├── DiscountSVIP.java       # SVIP 折扣 10%
│   │   ├── DiscountNormal.java     # 普通用户无折扣
│   │   └── DiscountStrategyRegistry.java
│   └── shipping/               # 运费策略
│       ├── ShippingStrategy.java
│       ├── ShippingCN.java         # 中国运费 ¥8
│       ├── ShippingUS.java         # 美国运费 $15
│       ├── ShippingJP.java         # 日本运费 ¥12
│       └── ShippingStrategyRegistry.java
├── handler/
│   ├── BasePriceHandler.java   # 基础定价
│   ├── DiscountHandler.java    # 折扣计算
│   ├── ShippingHandler.java    # 运费计算
│   ├── TaxHandler.java         # 税费计算
│   └── SummaryHandler.java     # 汇总结算
├── controller/
│   └── OrderController.java    # REST API 入口
└── OrderPipelineApplication.java  # Spring Boot 启动类
```

### 🧪 测试用例

```bash
# 运行所有测试
mvn -pl haifa-design-pattern test

# 仅运行集成测试
mvn -pl haifa-design-pattern test -Dtest="OrderPipelineIntegrationTest"
```

**测试场景覆盖：**

| 场景 | 国家 | 用户等级 | 原价 | 折扣 | 运费 | 税费 | 应付 |
|------|------|---------|------|------|------|------|------|
| VIP用户-中国 | CN | VIP | ¥100 | ¥5 | ¥8 | ¥5.70 | **¥108.70** |
| 普通用户-中国 | CN | NORMAL | ¥100 | ¥0 | ¥8 | ¥6.00 | **¥114.00** |
| SVIP用户-日本 | JP | SVIP | ¥200 | ¥20 | ¥12 | ¥0 | **¥192.00** |
| VIP用户-美国 | US | VIP | ¥100 | ¥5 | ¥15 | ¥0 | **¥110.00** |

### 🚀 扩展指南

#### 添加新的处理器

1. 实现 `OrderHandler` 接口
2. 添加 `@Component` 和 `@Order(n)` 注解
3. Spring 自动将其纳入处理链

```java
@Component
@Order(15)  // 在 BasePrice 之后，Discount 之前
public class RiskControlHandler implements OrderHandler {
    @Override
    public void handle(OrderContext ctx) {
        // 风控检查逻辑
    }
}
```

#### 添加新的策略

1. 实现对应的策略接口（如 `ShippingStrategy`）
2. 添加 `@Component` 注解
3. Spring 自动注册到策略表

```java
@Component
public class ShippingEU implements ShippingStrategy {
    @Override
    public String key() { return "EU"; }
    
    @Override
    public int calculateShipping(OrderContext ctx) {
        return 2000;  // 欧洲运费 20 元
    }
}
```

### 💡 设计优势

| 特性 | 说明 |
|------|------|
| **开闭原则** | 新增处理器/策略无需修改现有代码 |
| **单一职责** | 每个处理器专注单一计算逻辑 |
| **依赖倒置** | 通过接口解耦，便于单元测试 |
| **自动装配** | Spring IoC 消除手动注册的样板代码 |
| **可测试性** | 各组件可独立 Mock 测试 |

---

## 其他设计模式示例

- `chain/` - 职责链模式基础实现
- `pipeline/` - 管道模式
- `proxy/` - 代理模式

---

## 构建与运行

```bash
# 编译
mvn -pl haifa-design-pattern compile

# 测试
mvn -pl haifa-design-pattern test

# 运行 Spring Boot 应用（需解决父 POM 依赖冲突）
mvn -pl haifa-design-pattern spring-boot:run
```

## 依赖

- Java 21+
- Spring Boot 3.3.0
- JUnit 5

## License

MIT
