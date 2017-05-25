package me.wrj.haifa.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;

/**
 * Created by wangrenjun on 2017/5/25.
 */
public class RabbitMQRouterConcumer {

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(RabbitMQRouterProduct.EXCHANGE_NAME, "direct", true);
        //消费
        channel.queueDeclare(RabbitMQRouterProduct.QUEUE_NAME_EVEN, true, false, false, null);
        channel.queueDeclare(RabbitMQRouterProduct.QUEUE_NAME_ODD, true, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {

            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "',exchange=" + envelope.getExchange()
                                   + ",routing_key=" + envelope.getRoutingKey());
            }
        };
        channel.basicConsume(RabbitMQRouterProduct.QUEUE_NAME_EVEN, true, consumer);
        channel.basicConsume(RabbitMQRouterProduct.QUEUE_NAME_ODD, true, consumer);

        channel.close();
        connection.close();

    }

}
