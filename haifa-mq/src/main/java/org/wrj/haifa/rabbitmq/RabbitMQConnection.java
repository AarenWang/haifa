package org.wrj.haifa.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 连接到队列 然后发送消息
 */
public class RabbitMQConnection {

    public static final String QUEUE_NAME = "HAIFA_QUEUE";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        Connection conn = factory.newConnection();
        System.out.println("connection=" + conn);
        Channel channel = conn.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        String message = "Hello RabbitMQ Message";
        for (int i = 0; i < 10000; i++) {
            String news = message + "_" + i;
            channel.basicPublish("", QUEUE_NAME, null, news.getBytes());
        }

        channel.close();
        conn.close();

    }
}
