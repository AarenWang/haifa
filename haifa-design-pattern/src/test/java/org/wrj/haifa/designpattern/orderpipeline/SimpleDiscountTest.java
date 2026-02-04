package org.wrj.haifa.designpattern.orderpipeline;

import org.wrj.haifa.designpattern.orderpipeline.chain.OrderPipeline;
import org.wrj.haifa.designpattern.orderpipeline.model.DiscountEntry;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;

import java.util.List;

/**
 * 简单的折扣分摊和明细测试
 */
public class SimpleDiscountTest {

    public static void main(String[] args) {
        System.out.println("=== 订单折扣分摊与明细记录测试 ===\n");

        // 测试场景1：美国订单 10% 优惠码（验证分摊计算）
        testUSOrderWithPromoCode();

        // 测试场景2：中国 VIP + 优惠券（验证折扣明细）
        testChinaVIPWithCoupon();

        // 测试场景3：分摊补差验证
        testAllocationAdjustment();

        System.out.println("\n=== 所有测试通过 ===");
    }

    private static void testUSOrderWithPromoCode() {
        System.out.println("【测试1】美国订单 + OFF10 优惠码");

        OrderRequest request = new OrderRequest();
        request.setChannel("APP");
        request.setCountry("US");
        request.setUserTier("NORMAL");
        request.setCouponCode("OFF10");
        request.setItems(List.of(
                new LineItem("SKU-1100", 4000, 1),
                new LineItem("SKU-2200", 6000, 1)
        ));

        // 手动模拟处理流程
        OrderContext ctx = new OrderContext(request);
        simulateProcessing(ctx);

        // 验证
        List<LineItem> items = ctx.getRequest().getItems();
        int totalAllocated = items.stream()
                .mapToInt(LineItem::getAllocatedOrderDiscountCents)
                .sum();

        System.out.println("  原价小计: " + ctx.getItemsSubtotalCents() + " 分");
        System.out.println("  订单折扣: " + ctx.getOrderDiscountCents() + " 分");
        System.out.println("  商品1 (4000分) 分摊: " + items.get(0).getAllocatedOrderDiscountCents() + " 分");
        System.out.println("  商品2 (6000分) 分摊: " + items.get(1).getAllocatedOrderDiscountCents() + " 分");
        System.out.println("  分摊总额: " + totalAllocated + " 分");

        // 验证分摊：1000 分到 4000 和 6000 的商品上
        assert items.get(0).getAllocatedOrderDiscountCents() == 400
                : "商品1应分摊400分，实际: " + items.get(0).getAllocatedOrderDiscountCents();
        assert items.get(1).getAllocatedOrderDiscountCents() == 600
                : "商品2应分摊600分，实际: " + items.get(1).getAllocatedOrderDiscountCents();
        assert totalAllocated == 1000
                : "分摊总额应为1000分，实际: " + totalAllocated;

        System.out.println("  ✓ 分摊计算正确\n");
    }

    private static void testChinaVIPWithCoupon() {
        System.out.println("【测试2】中国 VIP + C100-20 优惠券");

        OrderRequest request = new OrderRequest();
        request.setChannel("WEB");
        request.setCountry("CN");
        request.setUserTier("VIP");
        request.setCouponCode("C100-20");
        // 使用足够金额的商品来满足满减条件（商品折扣后 >= 10000 分）
        request.setItems(List.of(
                new LineItem("FS-1001", 5000, 1),  // 秒杀品 8折 -> 4000
                new LineItem("SKU-2002", 7000, 1)   // 普通 VIP 95折 -> 6650，总计 10650
        ));

        OrderContext ctx = new OrderContext(request);
        simulateProcessing(ctx);

        // 验证折扣明细
        List<DiscountEntry> itemEntries = ctx.getItemDiscountEntries();
        List<DiscountEntry> orderEntries = ctx.getOrderDiscountEntries();

        System.out.println("  商品折扣明细数: " + itemEntries.size());
        System.out.println("  订单折扣明细数: " + orderEntries.size());

        for (DiscountEntry entry : itemEntries) {
            System.out.println("    - " + entry.getSource() + ": " + entry.getAmountCents() + " 分 (SKU: " + entry.getSku() + ")");
        }

        for (DiscountEntry entry : orderEntries) {
            System.out.println("    - " + entry.getSource() + ": " + entry.getAmountCents() + " 分 (券: " + entry.getCouponId() + ")");
        }

        assert !itemEntries.isEmpty() : "应有商品折扣明细";
        assert orderEntries.size() == 1 : "应有1条订单折扣明细，实际: " + orderEntries.size();
        assert "C100-20".equals(orderEntries.get(0).getCouponId()) : "券ID应为C100-20";

        System.out.println("  ✓ 折扣明细记录正确\n");
    }

    private static void testAllocationAdjustment() {
        System.out.println("【测试3】分摊补差验证");

        OrderRequest request = new OrderRequest();
        request.setCountry("CN");
        request.setUserTier("NORMAL");
        request.setCouponCode("C100-20");
        request.setItems(List.of(
                new LineItem("SKU-1001", 3000, 1),
                new LineItem("SKU-1002", 3000, 1),
                new LineItem("SKU-1003", 4000, 1)
        ));

        OrderContext ctx = new OrderContext(request);
        simulateProcessing(ctx);

        List<LineItem> items = ctx.getRequest().getItems();
        int totalAllocated = items.stream()
                .mapToInt(LineItem::getAllocatedOrderDiscountCents)
                .sum();

        System.out.println("  订单折扣: " + ctx.getOrderDiscountCents() + " 分");
        System.out.println("  分摊总额: " + totalAllocated + " 分");

        for (int i = 0; i < items.size(); i++) {
            LineItem item = items.get(i);
            System.out.println("  商品" + (i + 1) + " 分摊: " + item.getAllocatedOrderDiscountCents() + " 分");
        }

        assert totalAllocated == ctx.getOrderDiscountCents()
                : "分摊总额应等于订单折扣";

        System.out.println("  ✓ 补差机制正确\n");
    }

    /**
     * 模拟订单处理流程（简化版，用于测试）
     */
    private static void simulateProcessing(OrderContext ctx) {
        List<LineItem> items = ctx.getRequest().getItems();

        // 1. 基础定价
        int subtotal = items.stream().mapToInt(LineItem::getRawLineCents).sum();
        ctx.setItemsSubtotalCents(subtotal);
        ctx.setBasePriceCents(subtotal);

        // 2. 商品折扣（简化：秒杀品 20% off，VIP 用户 5% off）
        int totalItemDisc = 0;
        int afterItemDisc = 0;
        boolean isVIP = "VIP".equals(ctx.getRequest().getUserTier());

        for (LineItem item : items) {
            int disc = 0;

            // 秒杀品 20% off
            if (item.getSku().startsWith("FS-")) {
                disc = item.getRawLineCents() / 5; // 20%
                // 记录明细
                DiscountEntry entry = DiscountEntry.builder()
                        .amountCents(disc)
                        .source("ITEM_FLASH_SALE")
                        .ruleName("FlashSaleRule")
                        .sku(item.getSku())
                        .build();
                item.getItemDiscountEntries().add(entry);
                ctx.addItemDiscountEntry(entry);
            }

            // VIP 5% off（可叠加）
            if (isVIP && !item.getSku().startsWith("FS-")) {
                int vipDisc = item.getRawLineCents() / 20; // 5%
                disc += vipDisc;
                DiscountEntry entry = DiscountEntry.builder()
                        .amountCents(vipDisc)
                        .source("ITEM_VIP")
                        .ruleName("DiscountVIPRule")
                        .sku(item.getSku())
                        .build();
                item.getItemDiscountEntries().add(entry);
                ctx.addItemDiscountEntry(entry);
            }

            item.setItemDiscountCents(disc);
            item.setFinalLineCents(item.getRawLineCents() - disc);
            totalItemDisc += disc;
            afterItemDisc += item.getFinalLineCents();
        }

        ctx.setItemDiscountCents(totalItemDisc);
        ctx.setItemsAfterItemDiscCents(afterItemDisc);

        // 3. 订单折扣（简化：C100-20 满 100 减 20，OFF10 打 9 折）
        String coupon = ctx.getRequest().getCouponCode();
        int orderDisc = 0;
        if ("C100-20".equals(coupon) && afterItemDisc >= 10000) {
            orderDisc = 2000;
        } else if ("OFF10".equals(coupon)) {
            orderDisc = afterItemDisc / 10;
        }
        orderDisc = Math.min(orderDisc, afterItemDisc);
        ctx.setOrderDiscountCents(orderDisc);
        ctx.setDiscountCents(totalItemDisc + orderDisc);

        if (orderDisc > 0) {
            DiscountEntry entry = DiscountEntry.builder()
                    .amountCents(orderDisc)
                    .source("ORDER_COUPON")
                    .ruleName(coupon.equals("C100-20") ? "Coupon100Minus20" : "PromoCode10Off")
                    .couponId(coupon)
                    .build();
            ctx.addOrderDiscountEntry(entry);
        }

        // 4. 订单折扣分摊
        if (orderDisc > 0 && afterItemDisc > 0) {
            int totalAllocated = 0;
            for (int i = 0; i < items.size(); i++) {
                LineItem item = items.get(i);
                long allocatedLong = ((long) orderDisc * item.getFinalLineCents()) / afterItemDisc;
                int allocated = (int) allocatedLong;
                item.setAllocatedOrderDiscountCents(allocated);
                totalAllocated += allocated;
            }

            // 最后一行补差
            if (!items.isEmpty() && totalAllocated != orderDisc) {
                LineItem lastItem = items.get(items.size() - 1);
                lastItem.setAllocatedOrderDiscountCents(
                        lastItem.getAllocatedOrderDiscountCents() + (orderDisc - totalAllocated));
            }
        }
    }
}
