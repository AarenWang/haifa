package org.wrj.haifa.rabbitmq.spring.wms;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.wrj.haifa.rabbitmq.spring.api.OrderDelivereRequestDto;

import java.io.IOException;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class WMSMessageQueueListener implements ChannelAwareMessageListener {

    private static Logger     log = LoggerFactory.getLogger(WMSMessageQueueListener.class);

    @Autowired
    private LogisticesService logisticesService;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        log.debug("received message: type = {}, length = {}", message.getMessageProperties().getType(),
                  message.getBody().length);

        final String messageType = message.getMessageProperties().getType();

        log.info("received rabbitmq:[{}, {}]", messageType, new String(message.getBody(), "UTF-8"));

        try {
            OrderDelivereRequestDto requestDto = JSON.parseObject(message.getBody(), OrderDelivereRequestDto.class);
            logisticesService.deliveryRequestProcess(requestDto);
        } catch (Exception e) {
            log.error("consumer message execption", e);
            // 记录处理失败的消息
            log.warn("message read from mq: \n{}", new String(message.getBody(), "UTF-8"));
        } finally {
            try {
                // 不管是否发生异常,最终要确认消息被消费的情况
                // 如果因为处理异常而不确认消息,会导致MQ反复投递异常消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                log.error("ack message exception : {}", message.getMessageProperties().getDeliveryTag());
            }
        }
    }
}
