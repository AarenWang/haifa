package org.wrj.haifa.designpattern.orderpipeline.model;

/**
 * 订单上下文 - 在职责链中传递的上下文对象，包含计算过程中的各项费用
 * 
 * @author wrj
 */
public class OrderContext {

    /**
     * 原始订单请求
     */
    private final OrderRequest request;

    /**
     * 基础价格（单位：分）
     */
    private int basePriceCents;

    /**
     * 折扣金额（单位：分）
     */
    private int discountCents;

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
                ", discountCents=" + discountCents +
                ", shippingCents=" + shippingCents +
                ", taxCents=" + taxCents +
                ", payableCents=" + payableCents +
                '}';
    }
}
