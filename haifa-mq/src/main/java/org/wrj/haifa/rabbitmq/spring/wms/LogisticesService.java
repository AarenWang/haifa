package org.wrj.haifa.rabbitmq.spring.wms;

import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereRequestDto;
import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereResponseDto;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public interface LogisticesService {

    public void  deliveryRequestProcess(OrderDelivereRequestDto requestDto);


    public OrderDelivereResponseDto deliveryResponse(String omsOrderId);
}
