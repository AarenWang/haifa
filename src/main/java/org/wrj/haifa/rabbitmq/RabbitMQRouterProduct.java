package org.wrj.haifa.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Created by wangrenjun on 2017/5/25.
 */
public class RabbitMQRouterProduct {

    public static final String EXCHANGE_NAME    = "HAIFA_EXCHANGE";

    public static final String QUEUE_NAME_EVEN  = "HAIFA_QUEUE_EVEN";

    public static final String QUEUE_NAME_ODD   = "HAIFA_QUEUE_ODD";

    public static final String ROUTING_KEY_ODD  = "ROUTING_KEY_ODD";

    public static final String ROUTING_KEY_EVEN = "ROUTING_KEY_EVEN";

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
        channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
        channel.queueDeclare(QUEUE_NAME_ODD, true, false, false, null);
        channel.queueDeclare(QUEUE_NAME_EVEN, true, false, false, null);
        channel.queueBind(QUEUE_NAME_EVEN, EXCHANGE_NAME, ROUTING_KEY_EVEN);
        channel.queueBind(QUEUE_NAME_ODD, EXCHANGE_NAME, ROUTING_KEY_ODD);
        String message = "Hello RabbitMQ Message";
        for (int i = 0; i < 10000; i++) {
            String news = message + "_" + i;
            channel.basicPublish(EXCHANGE_NAME, i % 2 == 0 ? ROUTING_KEY_ODD : ROUTING_KEY_EVEN, null, news.getBytes());
        }

        channel.close();
        conn.close();

    }
}
