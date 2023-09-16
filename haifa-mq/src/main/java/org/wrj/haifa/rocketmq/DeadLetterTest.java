package org.wrj.haifa.rocketmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.time.LocalDateTime;
import java.util.List;

public class DeadLetterTest {

    public static void main(String[] args) {

        String nameSrvcAddress = "127.0.0.1:9876";

        String topic = "my_retry_topic";

        new Thread(new ProducerThread(nameSrvcAddress,topic,"order-app")).start();

        new Thread(new ConsumerThread(nameSrvcAddress,topic,"pay-app")).start();
    }


}

class ConsumerThread implements Runnable{

    private String namesrvAddr;
    private  String topic;

    private String consumerGropup;

    public ConsumerThread(String namesrvAddr,String topic,String consumerGropup){
        this.consumerGropup = consumerGropup;
        this.topic = topic;
        this.namesrvAddr = namesrvAddr;
    }

    @Override
    public void run() {
        try{
            // 指定消费组名为my-consumer
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGropup);
            // 配置namesrv地址
            consumer.setNamesrvAddr(namesrvAddr);
            // 订阅topic：myTopic001 下的全部消息（因为是*，*指定的是tag标签，代表全部消息，不进行任何过滤）
            consumer.subscribe(topic, "*");
            consumer.setMaxReconsumeTimes(3);
            // 注册监听器，进行消息消息。
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    for (MessageExt msg : msgs) {
                        String str = new String(msg.getBody());
                        // 输出消息内容
                        System.out.printf("tag:%s key:%s re_consumer_timer=%s body=%s \n",msg.getTags(),msg.getKeys(),msg.getReconsumeTimes(),str);
                    }
                    // 默认情况下，这条消息只会被一个consumer消费，这叫点对点消费模式。也就是集群模式。
                    // ack确认
                    int a = 1 / 0;
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            });
            // 启动消费者
            consumer.start();
            System.out.println("Consumer start");
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

class ProducerThread implements Runnable{

    private String namesrvAddr;
    private  String topic;

    private String producerGropup;

    public ProducerThread(String namesrvAddr,String topic,String producerGropup){
        this.producerGropup = producerGropup;
        this.topic = topic;
        this.namesrvAddr = namesrvAddr;
    }


    @Override
    public void run() {
        try {
            // 指定生产组名为my-producer
            DefaultMQProducer producer = new DefaultMQProducer(producerGropup);
            // 配置namesrv地址
            producer.setNamesrvAddr(namesrvAddr);
            // 启动Producer
            producer.start();
            for(int i =0; i < 10; i++){
                // 创建消息对象，topic为：myTopic001，消息内容为：hello world
                Message msg = new Message(topic, ("hello world "+ LocalDateTime.now()).getBytes());
                msg.setTags("order");
                msg.setKeys("bsafdsb");
                // 发送消息到mq，同步的
                SendResult result = producer.send(msg);
                System.out.println("发送消息成功！result is : " + result);

                Thread.sleep(2000L);

            }

            // 关闭Producer
            producer.shutdown();
            System.out.println("生产者 shutdown！");
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}


