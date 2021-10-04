package org.wrj.haifa.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;

import static org.wrj.haifa.rabbitmq.RabbitMQConnection.QUEUE_NAME;

/**
 * 连接到队列然后消费消息
 */
public class RabbitMQConsumer {

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {

            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);

        channel.close();
        connection.close();

    }
}
