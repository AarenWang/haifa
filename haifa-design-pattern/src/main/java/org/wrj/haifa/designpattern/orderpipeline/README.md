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
│   ├── itemdiscount/                    # 商品级折扣规则（可叠加）
│   │   ├── ItemDiscountRule.java
│   │   ├── FlashSaleRule.java           # 秒杀 8 折
│   │   ├── DiscountVIPRule.java         # VIP 95 折
│   │   └── DiscountNormalRule.java      # 兜底规则
│   └── orderdiscount/                   # 订单级折扣（互斥）
  ├── OrderDiscountStrategy.java
  ├── Coupon100Minus20.java        # 满 100-20 优惠券
  └── PromoCode10Off.java          # OFF10 促销码
├── handler/                             # 处理器实现
│   ├── BasePriceHandler.java            # @Order(10) 基础定价
│   ├── ItemDiscountHandler.java         # @Order(20) 商品折扣
│   ├── OrderDiscountHandler.java        # @Order(30) 订单折扣
│   ├── ShippingHandler.java             # @Order(40) 运费计算
│   ├── TaxHandler.java                  # @Order(45) 税费计算
│   └── SummaryHandler.java              # @Order(50) 汇总
└── controller/
    └── OrderController.java             # REST API
```

## 处理链顺序

| Order | Handler           | 说明                     |
|-------|-------------------|--------------------------|
| 10    | BasePriceHandler      | 汇总商品原价 / 兼容单价输入 |
| 20    | ItemDiscountHandler   | 跑商品级折扣规则（可叠加）   |
| 30    | OrderDiscountHandler  | 处理优惠券/促销码           |
| 40    | ShippingHandler       | 计算运费（策略模式）        |
| 45    | TaxHandler            | 计算税费                   |
| 50    | SummaryHandler        | 汇总应付金额               |

## 策略配置

### 运费策略

| Key | 实现类     | 说明                    |
|-----|------------|-------------------------|
| CN  | ShippingCN | 中国大陆，固定 8 元     |
| US  | ShippingUS | 美国，固定 15 USD       |
| JP  | ShippingJP | 日本，固定 1200 JPY     |

### 商品级折扣规则（可叠加）

| 顺序 | 实现类             | 说明                      |
|------|--------------------|---------------------------|
| 10   | FlashSaleRule      | SKU 以 FS- 开头享 8 折     |
| 20   | DiscountVIPRule    | VIP 用户额外 95 折         |
| 30   | DiscountNormalRule | 兜底：保持原价，可扩展其它 |

### 订单级折扣策略（互斥）

| Key      | 实现类             | 说明                                          |
|----------|--------------------|-----------------------------------------------|
| C100-20  | Coupon100Minus20   | 商品折后金额 ≥ 100 元时立减 20 元             |
| OFF10    | PromoCode10Off     | 任意订单按商品折后金额的 10% 进行促销码折扣   |

## 运行方式

### 启动应用

```bash
cd haifa-design-pattern
mvn spring-boot:run -Dstart-class=org.wrj.haifa.designpattern.orderpipeline.OrderPipelineApplication
```

### API 调用示例

```bash
# 中国 VIP 用户，下单 3 个商品并使用满减券
curl -X POST http://localhost:38080/order/quote \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "WEB",
    "country": "CN",
    "userTier": "VIP",
    "couponCode": "C100-20",
    "items": [
      { "sku": "FS-1001", "unitPriceCents": 5000, "qty": 1 },
      { "sku": "SKU-2002", "unitPriceCents": 3000, "qty": 1 },
      { "sku": "SKU-2003", "unitPriceCents": 2000, "qty": 2 }
    ]
  }'
```

### 预期返回

```json
{
  "basePriceCents": 12000,
  "itemsSubtotalCents": 12000,
  "itemDiscountCents": 1600,
  "itemsAfterItemDiscCents": 10400,
  "orderDiscountCents": 2000,
  "discountCents": 3600,
  "shippingCents": 800,
  "taxCents": 504,
  "payableCents": 9704,
  "request": {
    "channel": "WEB",
    "country": "CN",
    "userTier": "VIP",
    "couponCode": "C100-20",
    "items": [
      { "sku": "FS-1001", "unitPriceCents": 5000, "qty": 1 },
      { "sku": "SKU-2002", "unitPriceCents": 3000, "qty": 1 },
      { "sku": "SKU-2003", "unitPriceCents": 2000, "qty": 2 }
    ]
  }
}
```

## 计算说明

延续上面的示例：

1. **商品原价小计**：5000 + 3000 + 4000 = 12000 分
2. **商品级折扣**：
   - 秒杀商品：5000 × 20% = 1000 分；再叠加 VIP 5% = 250 分
   - 其他商品：VIP 5% 共减 350 分
   - 合计商品级折扣 1600 分，折后小计 10400 分
3. **订单级折扣**：10400 ≥ 10000，满减券再减 2000 分
4. **税费基数**：10400 - 2000 = 8400 分，按 6% = 504 分
5. **总计**：8400 + 运费 800 + 税 504 = **9704 分**

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
