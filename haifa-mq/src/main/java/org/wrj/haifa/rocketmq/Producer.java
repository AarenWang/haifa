package org.wrj.haifa.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.time.LocalDateTime;

public class Producer {

    public static void main(String[] args) throws Exception {
        // 指定生产组名为my-producer
        DefaultMQProducer producer = new DefaultMQProducer("my-producer");
        // 配置namesrv地址
        producer.setNamesrvAddr("127.0.0.1:9876");
        // 启动Producer
        producer.start();
        for(int i =0; i < 1000; i++){
            // 创建消息对象，topic为：myTopic001，消息内容为：hello world
            Message msg = new Message("test", ("hello world "+ LocalDateTime.now()).getBytes());
            msg.setTags("push");
            msg.setKeys("order");
            // 发送消息到mq，同步的
            SendResult result = producer.send(msg);
            System.out.println("发送消息成功！result is : " + result);

            Thread.sleep(2000L);

        }

        // 关闭Producer
        producer.shutdown();
        System.out.println("生产者 shutdown！");

    }


}
