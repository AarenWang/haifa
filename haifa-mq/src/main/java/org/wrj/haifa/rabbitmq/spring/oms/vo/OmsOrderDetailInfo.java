package org.wrj.haifa.rabbitmq.spring.oms.vo;

import java.math.BigDecimal;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class OmsOrderDetailInfo {

    //商品名称
    private String     goodsName;

    //商品数量
    private Integer    goodsCount;

    //商品价格
    private BigDecimal goodsPrice;

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public Integer getGoodsCount() {
        return goodsCount;
    }

    public void setGoodsCount(Integer goodsCount) {
        this.goodsCount = goodsCount;
    }

    public BigDecimal getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }
}
