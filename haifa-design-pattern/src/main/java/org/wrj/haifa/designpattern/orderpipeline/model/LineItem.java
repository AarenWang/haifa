package org.wrj.haifa.designpattern.orderpipeline.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品行项目 - 支持商品级折扣计算
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

    @Override
    public String toString() {
        return "LineItem{" +
                "sku='" + sku + '\'' +
                ", unitPriceCents=" + unitPriceCents +
                ", qty=" + qty +
                ", itemDiscountCents=" + itemDiscountCents +
                ", finalLineCents=" + finalLineCents +
                ", discountTags=" + discountTags +
                '}';
    }
}
