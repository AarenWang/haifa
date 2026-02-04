可以统一成一个思路：**规则（Rule）负责“算出候选折扣”**，**Policy 负责“怎么把候选折扣合成最终折扣”**。
这样您不需要在每个规则里塞叠加/互斥逻辑，也不用在 handler 里写一堆 if/else。

下面给您一套在 Spring IoC 下可落地的建模：**Rule + Candidate + Policy（叠加/互斥/取最大）**，并且支持将来扩展成“分组互斥 + 组内取最大 + 跨组叠加”这种电商常见组合。

---

## 1）统一数据结构：候选折扣 Candidate

```java
public record DiscountCandidate(
        String ruleId,          // 唯一标识，例如 "FLASH_SALE"
        String mutexGroup,      // 互斥组，例如 "PRICE_PROMO" / "COUPON" / null
        int priority,           // 优先级（可用于打破平手）
        int discountCents,      // 候选折扣金额（对当前行）
        boolean stackable       // 是否允许与其他候选叠加（用于更复杂策略）
) {}
```

> 关键点：**互斥不靠 supports / handler 写死**，而是候选上带 `mutexGroup` 元信息。

---

## 2）规则接口：只产出候选，不负责怎么合成

```java
public interface ItemDiscountRule {
    String id();
    boolean supports(OrderContext ctx, LineItem item);
    DiscountCandidate evaluate(OrderContext ctx, LineItem item);
}
```

示例：flashSale 属于 PRICE_PROMO 互斥组

```java
import org.springframework.stereotype.Component;

@Component
public class FlashSaleRule implements ItemDiscountRule {
    @Override public String id() { return "FLASH_SALE"; }

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return item.sku != null && item.sku.startsWith("FS-");
    }

    @Override
    public DiscountCandidate evaluate(OrderContext ctx, LineItem item) {
        int raw = item.unitPriceCents * item.qty;
        int d = (int) Math.round(raw * 0.20);
        return new DiscountCandidate(id(), "PRICE_PROMO", 100, d, false);
    }
}
```

示例：会员折扣也属于 PRICE_PROMO（互斥：通常“秒杀价 vs 会员价”二选一）

```java
import org.springframework.stereotype.Component;

@Component
public class VipPriceRule implements ItemDiscountRule {
    @Override public String id() { return "VIP_PRICE"; }

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return "VIP".equals(ctx.req.userTier);
    }

    @Override
    public DiscountCandidate evaluate(OrderContext ctx, LineItem item) {
        int raw = item.unitPriceCents * item.qty;
        int d = (int) Math.round(raw * 0.05);
        return new DiscountCandidate(id(), "PRICE_PROMO", 50, d, false);
    }
}
```

示例：加购赠券（可叠加的 item-level 立减），放到 ADDON 组，且 stackable=true

```java
import org.springframework.stereotype.Component;

@Component
public class AddOnVoucherRule implements ItemDiscountRule {
    @Override public String id() { return "ADDON_VOUCHER"; }

    @Override
    public boolean supports(OrderContext ctx, LineItem item) {
        return item.qty >= 2; // 举例：买2件送 3 元
    }

    @Override
    public DiscountCandidate evaluate(OrderContext ctx, LineItem item) {
        return new DiscountCandidate(id(), "ADDON", 10, 300, true);
    }
}
```

---

## 3）Policy：统一“叠加/互斥/取最大”三种模式

### Policy 接口 + 结果

```java
import java.util.List;

public record PolicyResult(int discountCents, List<DiscountCandidate> chosen) {}

public interface DiscountPolicy {
    PolicyResult apply(int lineRawCents, List<DiscountCandidate> candidates);
}
```

### A) 叠加（Stack All）

```java
import org.springframework.stereotype.Component;
import java.util.List;

@Component("STACK_ALL")
public class StackAllPolicy implements DiscountPolicy {
    @Override
    public PolicyResult apply(int lineRawCents, List<DiscountCandidate> candidates) {
        int sum = candidates.stream().mapToInt(DiscountCandidate::discountCents).sum();
        int finalDisc = Math.min(sum, lineRawCents);
        return new PolicyResult(finalDisc, candidates);
    }
}
```

### B) 取最大（Take Max）

```java
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component("TAKE_MAX")
public class TakeMaxPolicy implements DiscountPolicy {
    @Override
    public PolicyResult apply(int lineRawCents, List<DiscountCandidate> candidates) {
        DiscountCandidate best = candidates.stream()
                .max(Comparator.comparingInt(DiscountCandidate::discountCents)
                        .thenComparingInt(DiscountCandidate::priority))
                .orElse(null);

        if (best == null) return new PolicyResult(0, List.of());
        int finalDisc = Math.min(best.discountCents(), lineRawCents);
        return new PolicyResult(finalDisc, List.of(best));
    }
}
```

### C) 互斥（Mutex Pick One）

互斥本质也是“取最大”，但只在“同一互斥组内”。如果您要“全体互斥”，就把所有候选的 group 视为同一组。

```java
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component("MUTEX_BY_GROUP_TAKE_MAX")
public class MutexByGroupTakeMaxPolicy implements DiscountPolicy {

    @Override
    public PolicyResult apply(int lineRawCents, List<DiscountCandidate> candidates) {
        // 组内取最大，跨组叠加（电商最常见）
        Map<String, List<DiscountCandidate>> groups = candidates.stream()
                .collect(Collectors.groupingBy(c -> c.mutexGroup() == null ? "__DEFAULT__" : c.mutexGroup()));

        List<DiscountCandidate> chosen = new ArrayList<>();
        for (List<DiscountCandidate> group : groups.values()) {
            DiscountCandidate best = group.stream()
                    .max(Comparator.comparingInt(DiscountCandidate::discountCents)
                            .thenComparingInt(DiscountCandidate::priority))
                    .orElse(null);
            if (best != null) chosen.add(best);
        }

        int sum = chosen.stream().mapToInt(DiscountCandidate::discountCents).sum();
        int finalDisc = Math.min(sum, lineRawCents);
        return new PolicyResult(finalDisc, chosen);
    }
}
```

> 这一个 policy 就把三种模式统一了：

* **叠加**：所有候选都算
* **取最大**：全局选一个
* **互斥**：按组互斥（组内取最大，跨组叠加）

---

## 4）把 Policy 注入到 Handler：实现“可配置的折扣组合”

```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(20)
public class ItemDiscountHandler implements OrderHandler {

    private final List<ItemDiscountRule> rules;
    private final DiscountPolicy policy;

    public ItemDiscountHandler(
            List<ItemDiscountRule> rules,
            @Qualifier("MUTEX_BY_GROUP_TAKE_MAX") DiscountPolicy policy
    ) {
        this.rules = rules;
        this.policy = policy;
    }

    @Override
    public void handle(OrderContext ctx) {
        int subtotal = 0;
        int after = 0;

        for (LineItem item : ctx.req.items) {
            int raw = item.unitPriceCents * item.qty;
            subtotal += raw;

            List<DiscountCandidate> candidates = new ArrayList<>();
            for (ItemDiscountRule r : rules) {
                if (r.supports(ctx, item)) {
                    candidates.add(r.evaluate(ctx, item));
                }
            }

            PolicyResult result = policy.apply(raw, candidates);

            item.itemDiscountCents = result.discountCents();
            item.finalLineCents = raw - item.itemDiscountCents;
            // 记录命中的规则
            result.chosen().forEach(c -> item.discountTags.add(c.ruleId()));

            after += item.finalLineCents;
        }

        ctx.itemsSubtotalCents = subtotal;
        ctx.itemsAfterItemDiscCents = after;
    }
}
```

---

## 5）为什么这个建模“统一且可扩展”

### 核心优势

* 规则只做一件事：**产生候选折扣**（可测试、可回放）
* Policy 只做一件事：**合成候选**（叠加/互斥/取最大都只是不同策略）
* 互斥不再写死在代码结构里：通过 `mutexGroup` 元数据表达，您随时调整组划分

### 常见扩展（您大概率会需要）

1. **“组内取最大 + 跨组叠加”**（上面已支持）
2. **“某些组不能和某些组同用”**：在 Policy 里加“组间冲突矩阵”即可
3. **封顶**：PolicyResult 后做 `capCents`（比如单品折扣不超过 50%）
4. **最小价**：Policy 里强制 `finalLineCents >= minPriceCents`
5. **顺序依赖**（先算价格类，再算增量类）：用 group priority 分两轮 apply

---
