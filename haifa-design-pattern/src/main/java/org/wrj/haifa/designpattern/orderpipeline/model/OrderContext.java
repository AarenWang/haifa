package org.wrj.haifa.designpattern.orderpipeline.model;

/**
 * 订单上下文 - 在职责链中传递的上下文对象，包含计算过程中的各项费用
 * 支持两层折扣结构：商品级折扣 + 订单级折扣
 * 
 * @author wrj
 */
public class OrderContext {

    /**
     * 原始订单请求
     */
    private final OrderRequest request;

    /**
     * 基础价格（单位：分）- 兼容旧版
     */
    private int basePriceCents;

    /**
     * 折扣金额（单位：分）- 兼容旧版（= itemDiscountCents + orderDiscountCents）
     */
    private int discountCents;
    
    // ========== 新版两层折扣结构 ==========
    
    /**
     * 原价小计（所有行原价之和，单位：分）
     */
    private int itemsSubtotalCents;
    
    /**
     * 商品折扣后小计（所有行折后金额之和，单位：分）
     */
    private int itemsAfterItemDiscCents;
    
    /**
     * 商品级折扣总额（单位：分）
     */
    private int itemDiscountCents;
    
    /**
     * 订单级折扣（券/码等，单位：分）
     */
    private int orderDiscountCents;

    /**
     * 运费（单位：分）
     */
    private int shippingCents;

    /**
     * 税费（单位：分）
     */
    private int taxCents;

    /**
     * 最终应付金额（单位：分）
     */
    private int payableCents;

    public OrderContext(OrderRequest request) {
        this.request = request;
    }

    public OrderRequest getRequest() {
        return request;
    }

    public int getBasePriceCents() {
        return basePriceCents;
    }

    public void setBasePriceCents(int basePriceCents) {
        this.basePriceCents = basePriceCents;
    }

    public int getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(int discountCents) {
        this.discountCents = discountCents;
    }
    
    public int getItemsSubtotalCents() {
        return itemsSubtotalCents;
    }
    
    public void setItemsSubtotalCents(int itemsSubtotalCents) {
        this.itemsSubtotalCents = itemsSubtotalCents;
    }
    
    public int getItemsAfterItemDiscCents() {
        return itemsAfterItemDiscCents;
    }
    
    public void setItemsAfterItemDiscCents(int itemsAfterItemDiscCents) {
        this.itemsAfterItemDiscCents = itemsAfterItemDiscCents;
    }
    
    public int getItemDiscountCents() {
        return itemDiscountCents;
    }
    
    public void setItemDiscountCents(int itemDiscountCents) {
        this.itemDiscountCents = itemDiscountCents;
    }
    
    public int getOrderDiscountCents() {
        return orderDiscountCents;
    }
    
    public void setOrderDiscountCents(int orderDiscountCents) {
        this.orderDiscountCents = orderDiscountCents;
    }

    public int getShippingCents() {
        return shippingCents;
    }

    public void setShippingCents(int shippingCents) {
        this.shippingCents = shippingCents;
    }

    public int getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(int taxCents) {
        this.taxCents = taxCents;
    }

    public int getPayableCents() {
        return payableCents;
    }

    public void setPayableCents(int payableCents) {
        this.payableCents = payableCents;
    }

    @Override
    public String toString() {
        return "OrderContext{" +
                "request=" + request +
                ", basePriceCents=" + basePriceCents +
                ", itemsSubtotalCents=" + itemsSubtotalCents +
                ", itemDiscountCents=" + itemDiscountCents +
                ", itemsAfterItemDiscCents=" + itemsAfterItemDiscCents +
                ", orderDiscountCents=" + orderDiscountCents +
                ", discountCents=" + discountCents +
                ", shippingCents=" + shippingCents +
                ", taxCents=" + taxCents +
                ", payableCents=" + payableCents +
                '}';
    }
}
