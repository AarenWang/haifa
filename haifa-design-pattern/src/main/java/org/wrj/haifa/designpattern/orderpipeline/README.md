# 订单处理管道示例 - 策略模式 + 职责链模式 + Spring IoC

## 概述

这是一个演示**策略模式 + 职责链模式**组合、**运行在 Spring IoC 容器**中的完整例子。

### 业务场景

实现一套"订单风控 + 计价 + 优惠 + 运费 + 税费"的**可插拔处理链**。每个环节内部又可以按不同渠道/国家/会员等级选择不同**策略**。

## 设计模式说明

### 1. 职责链模式 (Chain of Responsibility)

- 一串 `OrderHandler`，每个 handler 做一件事
- 按顺序处理 `OrderContext`
- 通过 `@Order` 注解控制执行顺序

### 2. 策略模式 (Strategy)

- 某个 handler 内部根据条件选择一个策略实现
- 例如：运费策略（国内/国际）、折扣策略（VIP/普通）等
- 使用 `StrategyRegistry` 管理策略

### 3. Spring IoC 集成

- 所有 handler / strategy 都是 `@Component`，自动注入
- 用 `@Order` 控制链顺序
- 用"注册表（Registry）"方式把策略按 key 管理起来

## 项目结构

```
org.wrj.haifa.designpattern.orderpipeline
├── OrderPipelineApplication.java        # Spring Boot 启动类
├── model/
│   ├── OrderRequest.java                # 订单请求对象
│   └── OrderContext.java                # 订单上下文（链中传递）
├── chain/
│   ├── OrderHandler.java                # 处理器接口
│   └── OrderPipeline.java               # 处理管道（职责链执行器）
├── strategy/
│   ├── KeyedStrategy.java               # 带键策略接口
│   ├── StrategyRegistry.java            # 通用策略注册表
│   ├── shipping/                        # 运费策略
│   │   ├── ShippingStrategy.java
│   │   ├── ShippingCN.java              # 中国运费
│   │   ├── ShippingUS.java              # 美国运费
│   │   ├── ShippingJP.java              # 日本运费
│   │   └── ShippingStrategyRegistry.java
│   └── discount/                        # 折扣策略
│       ├── DiscountStrategy.java
│       ├── DiscountVIP.java             # VIP 折扣
│       ├── DiscountNormal.java          # 普通用户
│       ├── DiscountSVIP.java            # SVIP 超级会员
│       └── DiscountStrategyRegistry.java
├── handler/                             # 处理器实现
│   ├── BasePriceHandler.java            # @Order(10) 基础定价
│   ├── DiscountHandler.java             # @Order(20) 折扣计算
│   ├── ShippingHandler.java             # @Order(30) 运费计算
│   ├── TaxHandler.java                  # @Order(40) 税费计算
│   └── SummaryHandler.java              # @Order(50) 汇总
└── controller/
    └── OrderController.java             # REST API
```

## 处理链顺序

| Order | Handler           | 说明                     |
|-------|-------------------|--------------------------|
| 10    | BasePriceHandler  | 设置基础价格             |
| 20    | DiscountHandler   | 计算折扣（使用策略模式） |
| 30    | ShippingHandler   | 计算运费（使用策略模式） |
| 40    | TaxHandler        | 计算税费                 |
| 50    | SummaryHandler    | 汇总计算应付金额         |

## 策略配置

### 运费策略

| Key | 实现类     | 说明                    |
|-----|------------|-------------------------|
| CN  | ShippingCN | 中国大陆，固定 8 元     |
| US  | ShippingUS | 美国，固定 15 USD       |
| JP  | ShippingJP | 日本，固定 1200 JPY     |

### 折扣策略

| Key    | 实现类         | 说明                |
|--------|----------------|---------------------|
| NORMAL | DiscountNormal | 普通用户，无折扣    |
| VIP    | DiscountVIP    | VIP 用户，95 折     |
| SVIP   | DiscountSVIP   | SVIP 用户，90 折    |

## 运行方式

### 启动应用

```bash
cd haifa-design-pattern
mvn spring-boot:run -Dstart-class=org.wrj.haifa.designpattern.orderpipeline.OrderPipelineApplication
```

### API 调用示例

```bash
# 中国 VIP 用户，订单金额 100 元（10000 分）
curl -X POST http://localhost:8080/order/quote \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "WEB",
    "country": "CN",
    "userTier": "VIP",
    "amountCents": 10000
  }'
```

### 预期返回

```json
{
  "request": {
    "channel": "WEB",
    "country": "CN",
    "userTier": "VIP",
    "amountCents": 10000
  },
  "basePriceCents": 10000,
  "discountCents": 500,
  "shippingCents": 800,
  "taxCents": 570,
  "payableCents": 10870
}
```

## 计算说明

以上面的例子为例：

1. **基础价格**：10000 分（100 元）
2. **VIP 折扣**：10000 × 5% = 500 分
3. **中国运费**：固定 800 分（8 元）
4. **中国税费**：(10000 - 500) × 6% = 570 分
5. **应付金额**：(10000 - 500) + 800 + 570 = **10870 分**

## 扩展指南

### 添加新的运费策略

只需创建新的实现类，无需修改任何现有代码：

```java
@Component
public class ShippingEU implements ShippingStrategy {
    @Override public String key() { return "EU"; }
    
    @Override
    public int calcShippingCents(OrderContext ctx) {
        return 2000; // 欧盟运费 20 欧元
    }
}
```

### 添加新的处理器

```java
@Component
@Order(15) // 在 BasePriceHandler 之后，DiscountHandler 之前
public class RiskControlHandler implements OrderHandler {
    @Override
    public void handle(OrderContext ctx) {
        // 风控检查逻辑
    }
}
```

## 设计优势

1. **开闭原则**：添加新策略/处理器无需修改现有代码
2. **单一职责**：每个 handler 只负责一个环节
3. **易于测试**：策略和处理器都可以独立单元测试
4. **Spring 原生**：利用 `@Order` 和依赖注入，代码简洁

## 运行单元测试

```bash
mvn -pl haifa-design-pattern test
```
