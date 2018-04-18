package org.wrj.haifa.reenterable;

import java.math.BigDecimal;

/**
 * Created by wangrenjun on 2017/9/22.
 */
public class Purchase {

    private String     purchaseId;

    private String     goodName;

    private BigDecimal amount;

    public Purchase(String purchaseId, String goodName, BigDecimal amount) {
        this.purchaseId = purchaseId;
        this.goodName = goodName;
        this.amount = amount;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getGoodName() {
        return goodName;
    }

    public void setGoodName(String goodName) {
        this.goodName = goodName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    @Override
    public String toString() {
        return "Purchase{" +
                "purchaseId='" + purchaseId + '\'' +
                ", goodName='" + goodName + '\'' +
                ", amount=" + amount +
                '}';
    }
}
