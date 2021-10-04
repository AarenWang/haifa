package org.wrj.haifa.rabbitmq.spring.api;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class OrderDelivereResponseDto implements Serializable {

    // OMS系统订单ID
    private String    orderId;

    // WMS系统订单ID
    private String    wmsOrderId;

    // WMS系统发货时间
    private Timestamp wmsDeliverTime;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getWmsOrderId() {
        return wmsOrderId;
    }

    public void setWmsOrderId(String wmsOrderId) {
        this.wmsOrderId = wmsOrderId;
    }

    public Timestamp getWmsDeliverTime() {
        return wmsDeliverTime;
    }

    public void setWmsDeliverTime(Timestamp wmsDeliverTime) {
        this.wmsDeliverTime = wmsDeliverTime;
    }

    @Override
    public String toString() {
        return "OrderDelivereResponseDto{" + "orderId='" + orderId + '\'' + ", wmsOrderId='" + wmsOrderId + '\''
               + ", wmsDeliverTime=" + wmsDeliverTime + '}';
    }
}
