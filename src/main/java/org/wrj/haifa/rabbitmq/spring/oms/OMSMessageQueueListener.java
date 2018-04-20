package org.wrj.haifa.rabbitmq.spring.oms;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.wrj.haifa.rabbitmq.spring.wms.WMSMessageQueueListener;

import java.io.IOException;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class OMSMessageQueueListener implements ChannelAwareMessageListener {

    private static Logger log = LoggerFactory.getLogger(OMSMessageQueueListener.class);

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        log.debug("received message: type = {}, length = {}", message.getMessageProperties().getType(),
                  message.getBody().length);

        final String messageType = message.getMessageProperties().getType();

        log.info("received rabbitmq:[{}, {}]", messageType, new String(message.getBody(), "UTF-8"));

        try {
            // ResponseMsg responseMsg = JSON.parseObject(message.getBody(), ResponseMsg.class);
            // riskDetectService.handlerRiskResponse(responseMsg);
        } catch (Exception e) {
            log.error("failed to process message", e);
            // 记录处理失败的消息
            log.warn("message read from mq: \n{}", new String(message.getBody(), "UTF-8"));
        } finally {
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                log.error("ack message exception : {}", message.getMessageProperties().getDeliveryTag());
            }
        }
    }
}
