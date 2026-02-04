package org.wrj.haifa.designpattern.orderpipeline.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 折扣明细 - 用于审计与对账
 *
 * <p>记录每笔折扣的完整信息，便于排查问题、发票打印、退款计算等场景</p>
 *
 * @author wrj
 */
public class DiscountEntry {

    /**
     * 折扣金额（分），正数表示减免
     */
    private final int amountCents;

    /**
     * 折扣来源：ITEM_FLASH_SALE, ITEM_VIP, ORDER_COUPON, ORDER_PROMO 等
     */
    private final String source;

    /**
     * 规则名称：FlashSaleRule, DiscountVIPRule, Coupon100Minus20 等
     */
    private final String ruleName;

    /**
     * 券ID/促销码（可选）
     */
    private final String couponId;

    /**
     * 应用时间
     */
    private final LocalDateTime appliedAt;

    /**
     * 关联的 SKU（商品级折扣时填写）
     */
    private String sku;

    /**
     * 备注（可选）
     */
    private String remark;

    private DiscountEntry(Builder builder) {
        this.amountCents = builder.amountCents;
        this.source = builder.source;
        this.ruleName = builder.ruleName;
        this.couponId = builder.couponId;
        this.appliedAt = builder.appliedAt != null ? builder.appliedAt : LocalDateTime.now();
        this.sku = builder.sku;
        this.remark = builder.remark;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public int getAmountCents() {
        return amountCents;
    }

    public String getSource() {
        return source;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getCouponId() {
        return couponId;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public String getSku() {
        return sku;
    }

    public String getRemark() {
        return remark;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscountEntry that = (DiscountEntry) o;
        return amountCents == that.amountCents &&
                Objects.equals(source, that.source) &&
                Objects.equals(ruleName, that.ruleName) &&
                Objects.equals(couponId, that.couponId) &&
                Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountCents, source, ruleName, couponId, sku);
    }

    @Override
    public String toString() {
        return "DiscountEntry{" +
                "amountCents=" + amountCents +
                ", source='" + source + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", couponId='" + couponId + '\'' +
                ", appliedAt=" + appliedAt +
                ", sku='" + sku + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }

    /**
     * Builder 模式创建折扣明细
     */
    public static class Builder {
        private int amountCents;
        private String source;
        private String ruleName;
        private String couponId;
        private LocalDateTime appliedAt;
        private String sku;
        private String remark;

        public Builder amountCents(int amountCents) {
            this.amountCents = amountCents;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder couponId(String couponId) {
            this.couponId = couponId;
            return this;
        }

        public Builder appliedAt(LocalDateTime appliedAt) {
            this.appliedAt = appliedAt;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public DiscountEntry build() {
            return new DiscountEntry(this);
        }
    }
}
