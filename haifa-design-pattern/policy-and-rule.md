# Order Pipeline 折扣冲突治理设计：Rule / Candidate / Policy

本文档用于把“商品级优惠 + 订单级优惠”的**冲突（互斥/叠加/取最大/跨层互斥）**治理，从当前的“写死在 handler/规则里”升级为统一的 **Rule → Candidate → Policy** 模型，并与现有订单管道实现对齐、可渐进落地。

关联现有实现：
- 启动与包结构：见 [haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/OrderPipelineApplication.java](haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/OrderPipelineApplication.java)
- 当前商品折扣计算：见 [haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/handler/ItemDiscountHandler.java](haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/handler/ItemDiscountHandler.java)
- 当前订单折扣选择：见 [haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/handler/OrderDiscountHandler.java](haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/handler/OrderDiscountHandler.java)
- 当前订单折扣分摊：见 [haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/handler/OrderDiscountAllocationHandler.java](haifa-design-pattern/src/main/java/org/wrj/haifa/designpattern/orderpipeline/handler/OrderDiscountAllocationHandler.java)
- 先行草案（面向 item-level 的示例）：见 [haifa-design-pattern/expend.md](haifa-design-pattern/expend.md)

---

## 1. 背景与现状（As-Is）

### 1.1 管道结构（职责链）
当前 `OrderPipeline` 通过 Spring IoC 注入 `OrderHandler` 列表，并按 `@Order` 顺序执行。折扣相关的关键阶段：
1) ItemDiscountHandler：对每个 LineItem 遍历所有 `ItemDiscountRule`，**命中则累加折扣**，最后 `cap` 到 `lineRaw`  
2) OrderDiscountHandler：遍历 `OrderDiscountStrategy`，**命中第一个策略即 break（天然互斥）**  
3) OrderDiscountAllocationHandler：将 `orderDiscountCents` 按 `itemsAfterItemDisc` 占比分摊到各行（最后一行补差）

### 1.2 现状的典型问题
- **冲突策略分散**：商品级“叠加”写死在 handler，订单级“互斥”写死在遍历逻辑；一旦要变更（例如“秒杀价 vs VIP价 二选一”）需要改代码结构。
- **跨层冲突难表达**：例如“秒杀商品不允许再使用满减券”，目前只能在某个策略/handler 里硬编码判断。
- **审计可读性不足**：有 `DiscountEntry` 但缺少“为什么没选某个优惠”的原因（rejected reason/冲突原因/封顶原因）。
- **配置化困难**：想把某个国家/渠道的折扣规则从“叠加”切到“互斥取最大”，当前需要改 handler。

---

## 2. 目标与非目标（To-Be）

### 2.1 目标
- 统一建模：Rule 只负责产出候选折扣（Candidate），Policy 负责合并候选得到最终折扣。
- 可配置冲突治理：同一套 rules，不同 policy 即可实现“叠加/互斥/取最大/分组互斥跨组叠加”。
- 支持跨层冲突：item-level 与 order-level 可以通过“组/矩阵/约束”统一治理。
- 审计增强：输出 chosen 与 rejected（含原因），并映射到现有 `DiscountEntry` 结构。
- 渐进落地：优先替换商品级计算，再替换订单级选择；不强行重做运费/税费/分摊。

### 2.2 非目标
- 不引入完整规则引擎（DSL/表达式/在线编排）。
- 不涉及券核销、库存占用、分摊到促销预算科目等复杂域能力（只做计价冲突治理与审计）。

---

## 3. 统一概念模型

### 3.1 Rule（规则）：只产出候选，不决定“怎么用”
Rule 的职责：
- 判断是否适用（supports）
- 计算“如果我被采用，会产生多少折扣”（evaluate → Candidate）
- 不做互斥/叠加/取最大决策

> 对齐现有：目前 `ItemDiscountRule`/`OrderDiscountStrategy` 同时承担了“是否适用 + 计算 + 记录明细”的职责；To-Be 会把“组合决策”上移到 Policy。

### 3.2 Candidate（候选折扣）：携带冲突治理所需元信息
建议最小字段集合（可作为 record/POJO）：

- `id`：规则唯一标识（例如 `FLASH_SALE` / `VIP` / `C100-20` / `OFF10`）
- `scope`：作用域（`ITEM` 或 `ORDER`）
- `mutexGroup`：互斥组（例如 `PRICE_PROMO`、`ORDER_COUPON`、`PROMO_CODE`、`ADDON`）
- `priority`：优先级（用于打平/稳定性）
- `discountCents`：折扣金额（统一用“分”）
- `stackable`：是否允许与其他候选叠加（复杂 policy 可用）
- `meta`：扩展元数据（sku、couponCode、ruleName、预算归因、原因说明等）

可选增强字段（按需要逐步加）：
- `capType/capValue`：封顶（例如“最多 50%”、“最多减 20 元”）
- `minPayableCents`：最低成交价约束
- `exclusiveWithGroups`：跨组互斥（例如 `PRICE_PROMO` 与 `ORDER_COUPON` 不能同用）

### 3.3 Policy（策略）：把候选合成最终折扣
Policy 输入/输出建议包含“选择结果 + 可解释性”：

- 输入：`baseCents`（item raw 或 order subtotal）、`candidates`
- 输出：
  - `appliedDiscountCents`
  - `chosenCandidates`
  - `rejectedCandidates`（包含 reason：互斥淘汰/跨组冲突/封顶被截断/不满足阈值等）

> 对齐现有：`ItemDiscountHandler` 现在只有 applied 结果，没有 rejected；`OrderDiscountHandler` 只记录命中策略，没有“为什么没选其他策略”。

---

## 4. Policy 组合策略（核心能力）

以下 policy 覆盖绝大多数电商计价冲突需求：

### 4.1 STACK_ALL（全叠加）
- 所有 candidates 的折扣求和
- 最终折扣 `cap` 到 `baseCents`
- 适用于：加购立减、可叠加权益、当前 demo 的商品级行为（秒杀 + VIP 是叠加的）

### 4.2 TAKE_MAX（全局取最大）
- 从所有 candidates 中选择 `discountCents` 最大者（平手用 priority 打破）
- 适用于：同类优惠“只取一个最划算”

### 4.3 MUTEX_BY_GROUP_TAKE_MAX（组内互斥取最大，跨组叠加）
- 先按 `mutexGroup` 分组
- 每组内选一个最优
- 组间叠加并 cap
- 适用于：最常见的“价格类促销组内互斥、权益类可叠加”的组合

### 4.4 CROSS_SCOPE_CONFLICT（跨层冲突矩阵，可选）
在 4.1~4.3 的基础上增加“跨组/跨 scope 冲突矩阵”，例如：
- `PRICE_PROMO` 与 `ORDER_COUPON` 互斥（秒杀/特价不允许用满减券）
- `PROMO_CODE` 与 `ORDER_COUPON` 互斥（优惠码与券二选一）
- `VIP` 可以与 `ORDER_COUPON` 叠加（会员折扣允许叠加券）

实现思路：
- 先用基础 policy 选出候选集合
- 再应用冲突矩阵，把冲突组按优先级/折扣额淘汰
- 输出 rejected reason（例如 `CONFLICT_WITH_GROUP:ORDER_COUPON`）

---

## 5. 与现有代码的落地映射

### 5.1 商品级（ItemDiscountHandler）：从“写死叠加”升级为“候选 + policy”
现状（As-Is）：对每条 item，遍历所有 `ItemDiscountRule`，命中就 `disc += d`。

To-Be：
1) 遍历 rules，把命中的 rule 转为 candidates（每个 candidate 记录 ruleId、mutexGroup、discountCents 等）
2) 调用 `ItemDiscountPolicy.apply(lineRaw, candidates)` 得到 chosen + applied
3) 将 chosen 映射为 `DiscountEntry`（沿用现有 `createDiscountEntry` 或者在 rule 里提供 “entry factory”）
4) applied 写回 `item.itemDiscountCents`，并保持 `cap` 行为一致

关键收益：
- 不改 rule 本身，只换 policy 即可把“秒杀+VIP 叠加”切为“互斥二选一”。

### 5.2 订单级（OrderDiscountHandler）：从“命中第一个即互斥”升级为“候选 + policy”
现状（As-Is）：遍历 `OrderDiscountStrategy`，第一个 supports 的策略生效并 break。

To-Be：
1) 每个 strategy 产出 order-level candidate（如果不满足阈值则不产出或产出 0 并标注 reason）
2) 对所有 order candidates 应用 `OrderDiscountPolicy`：
   - 默认可以用 TAKE_MAX（在券/码中选最优）
   - 或 MUTEX_BY_GROUP（券组/码组互斥，允许与其他组叠加）
3) 输出 chosen/rejected，写入 `ctx.orderDiscountEntries`

说明：
- 这会改变“只按 @Order 优先级选第一个”的语义，升级为“可按金额/优先级选最优”，更贴近真实电商。

### 5.3 分摊（OrderDiscountAllocationHandler）：从“总额分摊”升级为“按 candidate 分摊”（可选增强）
现状：只按 `ctx.orderDiscountCents` 做一次总分摊，并把 order entry 复制到每行。

To-Be（增强）：
- 对每个 chosen order candidate 计算一个“订单折扣子总额”
- 分摊时按 candidate 粒度分别分摊，行上记录 `allocatedOrderDiscountEntries` 时保留 candidate id/source
- 好处：后续做预算归因/对账会更清晰

> 若短期不需要，保持现状也可以：policy 只决定 `ctx.orderDiscountCents` 与 entries，分摊逻辑不动。

---

## 6. 配置与注入建议（Spring IoC）

### 6.1 简单方式：@Qualifier 固定选择 policy
- `ItemDiscountHandler` 注入 `@Qualifier("STACK_ALL")` 或 `@Qualifier("MUTEX_BY_GROUP_TAKE_MAX")`
- `OrderDiscountHandler` 注入 `@Qualifier("TAKE_MAX")` 等

### 6.2 可配置方式：PolicyRegistry + properties
- 定义 `PolicyRegistry`（类似现有 `StrategyRegistry`），按 key 管理 policy bean
- 从配置读取：`orderpipeline.policy.item=MUTEX_BY_GROUP_TAKE_MAX`、`orderpipeline.policy.order=TAKE_MAX`
- handler 启动时从 registry 取 policy 实例

---

## 7. 迁移路线（最小风险）

1) 新增 Candidate/Policy/PolicyResult 抽象（先不改现有 rules）
2) 先在商品级落地：改造 `ItemDiscountHandler` 为“产候选 → 应用 policy”
   - 默认 policy 用 STACK_ALL，确保行为与当前测试一致
3) 再在订单级落地：改造 `OrderDiscountHandler` 为“产候选 → 应用 policy”
   - 初期可用 “TAKE_MAX” 但保持与当前策略一致（必要时通过 priority 兼容）
4) 如需跨层冲突，再引入冲突矩阵 policy（不影响前两步）
5) 最后再考虑“按 candidate 分摊”的增强

---

## 8. 测试与回归点

建议补充/保持以下验证（现有集成测试已覆盖一部分）：
- policy 单测：STACK_ALL / TAKE_MAX / MUTEX_BY_GROUP 的 chosen 与 applied
- 回归：cap 行为（折扣不超过 baseCents）
- 回归：订单折扣分摊总额精确等于订单折扣（含最后一行补差）
- 审计：chosen entries 数量与来源（item/order/allocated）可解释

---

## 9. FAQ

### Q1：supports 还要不要？
要。supports 用于“是否产生候选”；互斥/叠加/取最大不放在 supports 里。

### Q2：mutexGroup 与 stackable 有何区别？
- mutexGroup：解决“同组只能取一个”
- stackable：解决“即使不同组也可能不允许叠加”的复杂规则（更偏跨组限制）

### Q3：当前 demo 秒杀与 VIP 是叠加还是互斥？
当前实现与测试表现为叠加（商品级 handler 会对命中规则累加）。引入 policy 后可以通过配置切换语义：
- 叠加：STACK_ALL
- 互斥二选一：将两者置于同一 mutexGroup（如 PRICE_PROMO），使用 MUTEX_BY_GROUP_TAKE_MAX