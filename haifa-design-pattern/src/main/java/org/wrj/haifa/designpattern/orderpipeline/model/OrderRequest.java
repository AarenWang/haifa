package org.wrj.haifa.designpattern.orderpipeline.model;

/**
 * 订单请求对象 - 包含订单的输入信息
 * 
 * @author wrj
 */
public class OrderRequest {

    /**
     * 渠道: "WEB" / "APP" / ...
     */
    private String channel;

    /**
     * 国家: "CN" / "US" / ...
     */
    private String country;

    /**
     * 用户等级: "VIP" / "NORMAL"
     */
    private String userTier;

    /**
     * 订单金额（单位：分）
     */
    private int amountCents;

    public OrderRequest() {
    }

    public OrderRequest(String channel, String country, String userTier, int amountCents) {
        this.channel = channel;
        this.country = country;
        this.userTier = userTier;
        this.amountCents = amountCents;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUserTier() {
        return userTier;
    }

    public void setUserTier(String userTier) {
        this.userTier = userTier;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "channel='" + channel + '\'' +
                ", country='" + country + '\'' +
                ", userTier='" + userTier + '\'' +
                ", amountCents=" + amountCents +
                '}';
    }
}
