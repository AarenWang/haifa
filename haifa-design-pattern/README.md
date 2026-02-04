# haifa-design-pattern

设计模式学习与实践模块，包含多种经典设计模式的 Java 实现示例。

---

## 订单处理管道（Order Pipeline）

> **职责链模式 + 策略模式 + Spring IoC 混合设计模式示例**

这是一个电商订单计价系统的完整实现，演示如何将多种设计模式与 Spring 框架优雅结合，构建可扩展、可测试的业务流水线。

### 📋 业务场景

订单从创建到最终计算应付金额，需要经过多个处理环节：

```
订单请求 → 基础定价 → 商品折扣 → 订单折扣 → 运费计算 → 税费计算 → 汇总结算
```

每个环节的具体策略可能因用户等级、配送地区等因素而不同。

### 🏗️ 架构设计

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        OrderPipeline                                         │
│                     (Chain of Responsibility)                                │
├──────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌────────────┐  ┌────────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│  │BasePrice│→│ItemDiscount │→│OrderDiscount│→│Shipping  │→│   Tax   │→│ Summary │
│  │ Handler │  │  Handler   │  │  Handler   │  │ Handler │  │ Handler │  │ Handler │
│  └─────────┘  └────┬──────┘  └────┬───────┘  └────┬────┘  └─────────┘  └─────────┘
│                    │              │               │
│        ┌───────────┴────┐   ┌─────┴────┐   ┌──────┴──────┐
│        │ Item Rules     │   │ Coupon/  │   │ ShippingReg │  (Strategies)
│        │ (可叠加)        │   │ Promo    │   │            │
│        └────┬──────┬────┘   └────┬─────┘   └──────┬──────┘
│             │      │            │               │
│    ┌────────┴┐ ┌───┴───┐   ┌────┴────┐    ┌─────┴─────┐
│    │FlashSale│ │ VIP   │   │ C100-20 │    │    CN     │
│    │ 20% Off │ │ 5% Off│   │ OFF10   │    │    US     │
│    └────────┘ └───────┘   └─────────┘    └───────────┘
└──────────────────────────────────────────────────────────────────────────────┘
```

### 🆕 本轮改动（2026-02）

- 订单请求新增 `items[]` 与 `couponCode` 字段，兼容旧版 `amountCents`，可一次传多件商品。 
- 新增 `LineItem` 模型、`ItemDiscountHandler`（可叠加规则）与 `OrderDiscountHandler`（互斥策略），替换旧版单层折扣。 
- 新增 `itemdiscount/` 与 `orderdiscount/` 策略包（Flash Sale、VIP 95 折、满 100-20、OFF10 等），控制折扣的可插拔性。 
- REST 接口 `/order/quote` 返回分层金额（原价小计、商品折扣、订单折扣、税费、应付），默认端口更新为 `38080`。 
- 集成测试覆盖多件商品 + 优惠券、纯订单券、以及无商品明细的兼容路径，确保两层折扣与旧版场景都能通过。 

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
        if (ctx.getRequest().hasItems()) {
            int subtotal = ctx.getRequest().getItems().stream()
                    .mapToInt(LineItem::getRawLineCents)
                    .sum();
            ctx.setItemsSubtotalCents(subtotal);
            ctx.setBasePriceCents(subtotal);
        } else {
            int amount = ctx.getRequest().getAmountCents();
            ctx.setItemsSubtotalCents(amount);
            ctx.setBasePriceCents(amount);
        }
    }
}
```

**处理器执行顺序：**

| Order | Handler | 职责 |
|-------|---------|------|
| 10 | BasePriceHandler | 设置基础价格 |
| 20 | ItemDiscountHandler | 逐行商品折扣（flash sale、会员等叠加） |
| 30 | OrderDiscountHandler | 优惠券 / 促销码等订单级折扣 |
| 40 | ShippingHandler | 根据配送地区计算运费 |
| 45 | TaxHandler | 根据地区计算税费 |
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
│   ├── shipping/               # 运费策略
│   │   ├── ShippingStrategy.java
│   │   ├── ShippingCN.java         # 中国运费 ¥8
│   │   ├── ShippingUS.java         # 美国运费 $15
│   │   ├── ShippingJP.java         # 日本运费 ¥12
│   │   └── ShippingStrategyRegistry.java
│   ├── itemdiscount/           # 商品级折扣规则
│   │   ├── ItemDiscountRule.java
│   │   ├── FlashSaleRule.java      # 秒杀 8 折
│   │   ├── DiscountVIPRule.java    # VIP 95 折
│   │   └── DiscountNormalRule.java # 兜底
│   └── orderdiscount/          # 订单级折扣
│       ├── OrderDiscountStrategy.java
│       ├── Coupon100Minus20.java    # 满减券
│       └── PromoCode10Off.java      # 促销码
├── handler/
│   ├── BasePriceHandler.java   # 基础定价
│   ├── ItemDiscountHandler.java    # 商品折扣
│   ├── OrderDiscountHandler.java   # 订单折扣
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

| 场景 | 输入概述 | 商品级折扣 | 订单级折扣 | 运费 | 税费 | 应付 |
|------|----------|------------|------------|------|------|------|
| 中国 VIP 多商品 + C100-20 | 3 件商品（含秒杀 SKU）+ 满减券 | ¥16.00 | ¥20.00 | ¥8.00 | ¥5.04 | **¥97.04** |
| 美国普通用户 + OFF10 | 2 件常规商品 + 10% 优惠码 | ¥0.00 | ¥10.00 | ¥15.00 | ¥0.00 | **¥105.00** |
| 兼容旧版单金额 | `amountCents=10000`、无商品明细 | ¥0.00 | ¥0.00 | ¥8.00 | ¥6.00 | **¥114.00** |

### 🚀 扩展指南

#### 添加新的处理器

1. 实现 `OrderHandler` 接口
2. 添加 `@Component` 和 `@Order(n)` 注解
3. Spring 自动将其纳入处理链

```java
@Component
@Order(15)  // 在 BasePrice 之后，ItemDiscount 之前
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
