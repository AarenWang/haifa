package org.wrj.haifa.rabbitmq.spring.oms;

import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereResponseDto;
import org.wrj.haifa.rabbitmq.spring.oms.vo.OmsOrderInfo;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public interface OrderService {

    /**
     * 通知WMS发货
     * @param omsOrderInfo
     * @return
     */
    public String notifyWmsDelivery(OmsOrderInfo omsOrderInfo);


    /**
     * 处理WMS发货响应通知
     * @param rspDto
     */
    public void  processWmsOrderDeliveryNotify(OrderDelivereResponseDto rspDto);


}
