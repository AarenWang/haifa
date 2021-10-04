package org.wrj.haifa.rabbitmq.spring.wms;

import org.apache.log4j.spi.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereRequestDto;
import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereResponseDto;

/**
 * Created by wangrenjun on 2018/4/20.
 */
@Service
public class LogisticesServiceImpl implements LogisticesService {

    private static Logger  logger = org.slf4j.LoggerFactory.getLogger(LogisticesServiceImpl.class);
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void deliveryRequestProcess(OrderDelivereRequestDto requestDto) {
        logger.info("deliveryRequestProcess OrderDelivereRequestDto = "+requestDto);


    }

    @Override
    public OrderDelivereResponseDto deliveryResponse(String omsOrderId) {
        return null;
    }
}
