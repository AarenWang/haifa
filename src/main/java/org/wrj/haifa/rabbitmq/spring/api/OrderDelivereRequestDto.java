package org.wrj.haifa.rabbitmq.spring.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class OrderDelivereRequestDto implements Serializable{

    //订单编号
    private  String orderId;

    //订单支付时间
    private Timestamp orderPayTime;

    //订单支付金额
    private  BigDecimal payAmount;

    //收货人姓名
    private  String receiverName;

    //收货人地址
    private  String receiverAddress;

    //商品明细列表
    private List<GoodsInfo> goodsInfoList;

    public Timestamp getOrderPayTime() {
        return orderPayTime;
    }

    public void setOrderPayTime(Timestamp orderPayTime) {
        this.orderPayTime = orderPayTime;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public List<GoodsInfo> getGoodsInfoList() {
        return goodsInfoList;
    }

    public void setGoodsInfoList(List<GoodsInfo> goodsInfoList) {
        this.goodsInfoList = goodsInfoList;
    }

    @Override
    public String toString() {
        return "OrderDelivereRequestDto{" +
                "orderId='" + orderId + '\'' +
                ", orderPayTime=" + orderPayTime +
                ", payAmount=" + payAmount +
                ", receiverName='" + receiverName + '\'' +
                ", receiverAddress='" + receiverAddress + '\'' +
                ", goodsInfoList=" + goodsInfoList +
                '}';
    }
}
