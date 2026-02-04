package org.wrj.haifa.designpattern.orderpipeline.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单请求对象 - 包含订单的输入信息
 * 支持多商品行和优惠券/折扣码
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
     * 用户等级: "VIP" / "NORMAL" / "SVIP"
     */
    private String userTier;

    /**
     * 订单金额（单位：分）- 兼容旧版单商品模式
     */
    private int amountCents;
    
    /**
     * 优惠券/折扣码: "C100-20" / "OFF10" / ...
     */
    private String couponCode;
    
    /**
     * 商品行列表 - 新版多商品模式
     */
    private List<LineItem> items = new ArrayList<>();

    public OrderRequest() {
    }

    public OrderRequest(String channel, String country, String userTier, int amountCents) {
        this.channel = channel;
        this.country = country;
        this.userTier = userTier;
        this.amountCents = amountCents;
    }
    
    /**
     * 判断是否为多商品模式
     */
    public boolean hasItems() {
        return items != null && !items.isEmpty();
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
    
    public String getCouponCode() {
        return couponCode;
    }
    
    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
    
    public List<LineItem> getItems() {
        return items;
    }
    
    public void setItems(List<LineItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "channel='" + channel + '\'' +
                ", country='" + country + '\'' +
                ", userTier='" + userTier + '\'' +
                ", amountCents=" + amountCents +
                ", couponCode='" + couponCode + '\'' +
                ", items=" + items +
                '}';
    }
}
