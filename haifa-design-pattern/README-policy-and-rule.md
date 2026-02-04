# 订单优惠 Rule / Candidate / Policy 设计与实现说明

本文档基于 `haifa-design-pattern/expend.md`、`haifa-design-pattern/policy-and-rule.md` 以及 `org.wrj.haifa.designpattern.orderpolicyrule` 相关源码，说明订单优惠业务的核心概念、冲突治理方式与落地实现细节。

**适用范围**
- 商品级优惠与订单级优惠的冲突治理
- 需要支持叠加、互斥、取最大、跨组冲突矩阵的电商计价
- 希望保留审计信息（chosen / rejected）的实现

**核心结论**
- Rule 只产出候选折扣，不做组合决策
- Policy 负责“怎么选、怎么合成”候选折扣
- Candidate 携带互斥组与元数据，冲突治理不再写死在规则或 handler 里

---

**业务背景与痛点**
- 商品级规则常见玩法：秒杀价、会员价、加购赠券
- 订单级规则常见玩法：满减券、优惠码
- 传统实现把“叠加/互斥/取最大”写死在 handler 或规则里
- 规则扩展时容易引入跨层冲突判断和 if/else 扩散

本模块的目标是统一冲突治理：把“是否生效”交给 Rule，把“如何组合”交给 Policy。

---

**统一概念模型**

**1) Rule：只产出候选，不决定最终使用方式**
- 商品级规则接口：`ItemDiscountRule`
- 订单级规则接口：`OrderDiscountRule`
- 规则只实现 `supports()` 与 `calcDiscountCents()`，组合逻辑由 Policy 决定

**2) Candidate：候选折扣的最小决策单元**
- 实现类：`DiscountCandidate`
- 关键字段
- `id` 规则唯一标识
- `scope` 作用域：`ITEM` / `ORDER`
- `mutexGroup` 互斥组
- `priority` 优先级
- `discountCents` 折扣金额（分）
- `stackable` 是否允许叠加（当前实现保留元数据，未直接参与基础 policy 计算）
- `meta` 扩展信息（如 sku、ruleName）
- `entry` 对接现有 `DiscountEntry`

**3) Policy：候选折扣合成策略**
- 接口：`DiscountPolicy.apply(baseCents, candidates)`
- 输出：`PolicyResult`，包含 applied、chosen、rejected、capApplied

---

**现有规则与分组设计**

**商品级规则**
- `FlashSaleRule`
- 规则：SKU 以 `FS-` 开头，20% off
- 互斥组：`PRICE_PROMO`
- 优先级：100
- `stackable=false`

- `VipPriceRule`
- 规则：VIP 用户 5% off
- 互斥组：`PRICE_PROMO`
- 优先级：50
- `stackable=false`

- `AddOnVoucherRule`
- 规则：买 2 件送 3 元
- 互斥组：`ADDON`
- 优先级：10
- `stackable=true`

**订单级规则**
- `Coupon100Minus20Rule`
- 规则：券码 `C100-20`，满 100 减 20
- 互斥组：`ORDER_COUPON`
- 优先级：100
- `stackable=false`

- `PromoCode10OffRule`
- 规则：优惠码 `PROMO10`，10% off
- 互斥组：`PROMO_CODE`
- 优先级：50
- `stackable=false`

---

**Policy 体系与冲突治理策略**

**1) STACK_ALL（全叠加）**
- 实现：`StackAllPolicy`
- 行为：所有候选折扣求和，超过 `baseCents` 则封顶
- 适用场景：可叠加权益、加购赠券等

**2) TAKE_MAX（全局取最大）**
- 实现：`TakeMaxPolicy`
- 行为：按 `discountCents` + `priority` 选最大候选
- 被淘汰的候选会进入 rejected，原因 `TAKE_MAX_REJECTED`

**3) MUTEX_BY_GROUP_TAKE_MAX（组内互斥，跨组叠加）**
- 实现：`MutexByGroupTakeMaxPolicy`
- 行为：按 `mutexGroup` 分组，组内取最大，跨组求和后封顶
- 组内未被选中的候选会进入 rejected，原因 `GROUP_MUTEX_REJECTED`
- `mutexGroup` 为空时归入 `__DEFAULT__`

**4) CONFLICT_MATRIX（跨组冲突矩阵）**
- 实现：`ConflictMatrixPolicy`
- 行为：在基础 policy 的 chosen 结果上做二次冲突过滤
- 冲突判断来自 `ConflictMatrix`
- 被冲突淘汰的候选会进入 rejected，原因 `CONFLICT_WITH_GROUP:<group>`

---

**执行流程（OrderPolicyEngine）**

**阶段 1：商品级优惠计算**
1. 遍历订单行 `LineItem`
2. 对每条 item 执行所有 `ItemDiscountRule`，产出候选
3. 调用 `itemPolicy.apply(lineRaw, candidates)` 得到 `PolicyResult`
4. 写回 `itemDiscountCents` 与 `finalLineCents`
5. 记录 `DiscountEntry` 与 `discountTags`
6. 汇总 `itemsSubtotalCents`、`itemsAfterItemDiscCents`、`itemDiscountCents`

**阶段 2：订单级优惠计算**
1. 遍历 `OrderDiscountRule` 产出 order-level 候选
2. 调用 `orderPolicy.apply(itemsAfterItemDiscCents, candidates)`
3. 写回 `orderDiscountCents` 与总折扣 `discountCents`
4. 记录订单级 `DiscountEntry`

**执行结果**
- `PolicyExecutionResult` 包含
- 商品级每行的 `ItemPolicyDecision`
- 订单级 `PolicyResult`

---

**配置与扩展方式**

**Policy key 机制**
- 所有内置 policy 实现 `KeyedPolicy`，提供 `key()`
- 使用 `PolicyRegistry` 按 key 获取策略
- 内置 key
- `STACK_ALL`
- `TAKE_MAX`
- `MUTEX_BY_GROUP_TAKE_MAX`

**扩展方向**
- 扩展 `DiscountCandidate` 的 `meta` 字段用于审计或预算归因
- 扩展 `DiscountPolicy` 实现，例如加入封顶规则、最低成交价约束
- 使用 `ConflictMatrix` 表达跨层冲突矩阵

---

**示例：跨层冲突矩阵配置**

示例目标：价格类促销（`PRICE_PROMO`）不允许叠加订单券（`ORDER_COUPON`），但允许与优惠码（`PROMO_CODE`）叠加。

```java
ConflictMatrix matrix = new ConflictMatrix()
    .addConflict("PRICE_PROMO", "ORDER_COUPON");

DiscountPolicy basePolicy = new MutexByGroupTakeMaxPolicy();
DiscountPolicy policyWithMatrix = new ConflictMatrixPolicy(basePolicy, matrix);

// item policy 或 order policy 任一侧都可以组合该策略
```

说明：
- 冲突矩阵是在基础 policy 选出来的 chosen 集合上做二次过滤
- 被淘汰的候选会进入 rejected，并带上 `CONFLICT_WITH_GROUP:<group>` 原因

---

**示例：构建执行引擎**

```java
List<ItemDiscountRule> itemRules = List.of(
    new FlashSaleRule(),
    new VipPriceRule(),
    new AddOnVoucherRule()
);

List<OrderDiscountRule> orderRules = List.of(
    new Coupon100Minus20Rule(),
    new PromoCode10OffRule()
);

DiscountPolicy itemPolicy = new MutexByGroupTakeMaxPolicy();
DiscountPolicy orderPolicy = new TakeMaxPolicy();

OrderPolicyEngine engine = new OrderPolicyEngine(itemRules, orderRules, itemPolicy, orderPolicy);
PolicyExecutionResult result = engine.execute(context);
```

---

**测试覆盖**
- BDD 测试：`haifa-design-pattern/src/test/java/org/wrj/haifa/designpattern/orderpolicyrule/bdd/OrderPolicyEngineSteps.java`
- 覆盖内容包含
- item 级别的 chosen / rejected 候选判断
- 订单级策略选择与最终折扣
- policy key 的切换与矩阵冲突策略

---

**BDD 场景示例（可写入 feature 文件）**

示例 1：组内互斥，跨组叠加（`MUTEX_BY_GROUP_TAKE_MAX`）

```gherkin
Scenario: PRICE_PROMO 组内互斥取最大，ADDON 组可叠加
  Given a user tier "VIP"
  And item discount rules: "FlashSaleRule, VipPriceRule, AddOnVoucherRule"
  And item policy "MUTEX_BY_GROUP_TAKE_MAX"
  And the order has items:
    | sku  | unitPriceCents | qty |
    | FS-1 | 10000          | 2   |
  When the policy engine executes
  Then chosen item candidate ids for "FS-1" should be:
    | id           |
    | FLASH_SALE   |
    | ADDON_VOUCHER|
```

说明：
- `FLASH_SALE` 与 `VIP_PRICE` 同属 `PRICE_PROMO`，组内只取一条
- `ADDON_VOUCHER` 属于 `ADDON`，可与 `PRICE_PROMO` 组叠加

示例 2：全局取最大（`TAKE_MAX`）

```gherkin
Scenario: TAKE_MAX 只保留最大折扣
  Given a user tier "VIP"
  And item discount rules: "FlashSaleRule, VipPriceRule, AddOnVoucherRule"
  And item policy "TAKE_MAX"
  And the order has items:
    | sku  | unitPriceCents | qty |
    | FS-2 | 10000          | 2   |
  When the policy engine executes
  Then chosen item candidate ids for "FS-2" should be:
    | id         |
    | FLASH_SALE |
  And rejected item candidate ids for "FS-2" should be:
    | id            | reason             |
    | VIP_PRICE     | TAKE_MAX_REJECTED  |
    | ADDON_VOUCHER | TAKE_MAX_REJECTED  |
```

说明：
- `TAKE_MAX` 只保留最大折扣候选，其余进入 rejected
- 若需要验证订单级 chosen/rejected，可在 Steps 中补充对应断言

---

**与现有订单管道的衔接**
- 该实现复用 `orderpipeline` 的 `OrderContext`、`LineItem`、`DiscountEntry`
- 规则与 policy 的组合逻辑可逐步替换原有 handler 中“写死的叠加/互斥”逻辑
- 可先在商品级落地，再切换订单级策略，保持迁移可控
