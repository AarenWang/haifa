package org.wrj.haifa.rabbitmq.spring.oms.vo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class OmsOrderInfo {

    //订单编号
    private  String orderId;

    //订单支付时间
    private Timestamp orderPayTime;

    //订单支付金额
    private BigDecimal payAmount;

    //收货人姓名
    private  String receiverName;

    //收货人地址
    private  String receiverAddress;

    //商品明细列表
    private List<OmsOrderDetailInfo> orderDetailInfoList;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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

    public List<OmsOrderDetailInfo> getOrderDetailInfoList() {
        return orderDetailInfoList;
    }

    public void setOrderDetailInfoList(List<OmsOrderDetailInfo> orderDetailInfoList) {
        this.orderDetailInfoList = orderDetailInfoList;
    }

    @Override
    public String toString() {
        return "OmsOrderInfo{" +
                "orderId='" + orderId + '\'' +
                ", orderPayTime=" + orderPayTime +
                ", payAmount=" + payAmount +
                ", receiverName='" + receiverName + '\'' +
                ", receiverAddress='" + receiverAddress + '\'' +
                ", orderDetailInfoList=" + orderDetailInfoList +
                '}';
    }
}
