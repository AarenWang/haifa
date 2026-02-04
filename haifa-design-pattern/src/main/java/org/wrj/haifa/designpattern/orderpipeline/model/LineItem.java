package org.wrj.haifa.designpattern.orderpipeline.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品行项目 - 支持商品级折扣计算、折扣明细记录、订单折扣分摊
 */
public class LineItem {

    private String sku;
    private int unitPriceCents;
    private int qty;

    // 商品级折扣（总额，不是单价）
    private int itemDiscountCents;
    // 折后行金额
    private int finalLineCents;
    // 记录折扣原因：FLASH_SALE, VIP_DISCOUNT 等
    private List<String> discountTags = new ArrayList<>();

    // ========== 新增：折扣明细与分摊 ==========

    /**
     * 商品级折扣明细列表
     */
    private List<DiscountEntry> itemDiscountEntries = new ArrayList<>();

    /**
     * 分摊到该行的订单折扣（用于发票/退款/税基计算）
     */
    private int allocatedOrderDiscountCents;

    /**
     * 订单级折扣明细列表（分摊到该行）
     */
    private List<DiscountEntry> allocatedOrderDiscountEntries = new ArrayList<>();
    
    public LineItem() {}
    
    public LineItem(String sku, int unitPriceCents, int qty) {
        this.sku = sku;
        this.unitPriceCents = unitPriceCents;
        this.qty = qty;
    }
    
    /**
     * 计算原价行金额
     */
    public int getRawLineCents() {
        return unitPriceCents * qty;
    }

    // Getters and Setters
    
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(int unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getItemDiscountCents() {
        return itemDiscountCents;
    }

    public void setItemDiscountCents(int itemDiscountCents) {
        this.itemDiscountCents = itemDiscountCents;
    }

    public int getFinalLineCents() {
        return finalLineCents;
    }

    public void setFinalLineCents(int finalLineCents) {
        this.finalLineCents = finalLineCents;
    }

    public List<String> getDiscountTags() {
        return discountTags;
    }

    public void setDiscountTags(List<String> discountTags) {
        this.discountTags = discountTags;
    }

    /**
     * 获取该行总折扣（商品级 + 分摊的订单级）
     */
    public int getTotalDiscountCents() {
        return itemDiscountCents + allocatedOrderDiscountCents;
    }

    /**
     * 获取最终应付行金额（原价 - 商品折扣 - 分摊的订单折扣）
     */
    public int getFinalPayableLineCents() {
        return getRawLineCents() - itemDiscountCents - allocatedOrderDiscountCents;
    }

    // ========== 新增字段的 Getters and Setters ==========

    public List<DiscountEntry> getItemDiscountEntries() {
        return itemDiscountEntries;
    }

    public void setItemDiscountEntries(List<DiscountEntry> itemDiscountEntries) {
        this.itemDiscountEntries = itemDiscountEntries;
    }

    public int getAllocatedOrderDiscountCents() {
        return allocatedOrderDiscountCents;
    }

    public void setAllocatedOrderDiscountCents(int allocatedOrderDiscountCents) {
        this.allocatedOrderDiscountCents = allocatedOrderDiscountCents;
    }

    public List<DiscountEntry> getAllocatedOrderDiscountEntries() {
        return allocatedOrderDiscountEntries;
    }

    public void setAllocatedOrderDiscountEntries(List<DiscountEntry> allocatedOrderDiscountEntries) {
        this.allocatedOrderDiscountEntries = allocatedOrderDiscountEntries;
    }

    @Override
    public String toString() {
        return "LineItem{" +
                "sku='" + sku + '\'' +
                ", unitPriceCents=" + unitPriceCents +
                ", qty=" + qty +
                ", itemDiscountCents=" + itemDiscountCents +
                ", finalLineCents=" + finalLineCents +
                ", discountTags=" + discountTags +
                ", allocatedOrderDiscountCents=" + allocatedOrderDiscountCents +
                ", itemDiscountEntries=" + itemDiscountEntries +
                ", allocatedOrderDiscountEntries=" + allocatedOrderDiscountEntries +
                '}';
    }
}
