package org.wrj.haifa.rabbitmq.spring.oms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wrj.haifa.rabbitmq.spring.api.GoodsInfo;
import org.wrj.haifa.rabbitmq.spring.api.MessageConstant;
import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereRequestDto;
import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereResponseDto;
import org.wrj.haifa.rabbitmq.spring.oms.vo.OmsOrderDetailInfo;
import org.wrj.haifa.rabbitmq.spring.oms.vo.OmsOrderInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangrenjun on 2018/4/20.
 */
@Service
public class OrderServiceImpl implements  OrderService{

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public String notifyWmsDelivery(OmsOrderInfo omsOrderInfo) {
        logger.info("notifyWmsDelivery omsOrderInfo = "+omsOrderInfo);

        OrderDelivereRequestDto requestDto = new OrderDelivereRequestDto();
        requestDto.setReceiverAddress(omsOrderInfo.getReceiverAddress());
        requestDto.setReceiverName(omsOrderInfo.getReceiverName());
        requestDto.setOrderId(omsOrderInfo.getOrderId());
        requestDto.setOrderPayTime(omsOrderInfo.getOrderPayTime());

        List<GoodsInfo> goodsInfoList = new ArrayList<>();
        for(OmsOrderDetailInfo info : omsOrderInfo.getOrderDetailInfoList()){
            GoodsInfo goodsInfo = new GoodsInfo();
            goodsInfo.setGoodsCount(info.getGoodsCount());
            goodsInfo.setGoodsName(info.getGoodsName());
            goodsInfo.setGoodsPrice(info.getGoodsPrice());
            goodsInfoList.add(goodsInfo);
        }
        requestDto.setGoodsInfoList(goodsInfoList);

        rabbitTemplate.convertAndSend(MessageConstant.OMS_EXCHANGE,MessageConstant.OMS_ORDER_CREATE_REQUEST,requestDto);
        return null;
    }

    @Override
    public void processWmsOrderDeliveryNotify(OrderDelivereResponseDto rspDto) {

    }
}
