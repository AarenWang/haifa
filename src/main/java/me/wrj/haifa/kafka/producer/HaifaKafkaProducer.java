package me.wrj.haifa.kafka.producer;

import kafka.producer.KeyedMessage;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * Created by wangrenjun on 2017/4/6.
 */
public class HaifaKafkaProducer {

    public static void main(String[] args) {

        Random rnd = new Random();
        int events = 100000;

        // 设置配置属性
        Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("metadata.broker.list", "127.0.0.1:9092");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        // key.serializer.class默认为serializer.class
        // props.put("key.serializer.class", "kafka.serializer.StringEncoder");

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // 可选配置，如果不配置，则使用默认的partitioner
        // props.put("partitioner.class", "com.catt.kafka.demo.PartitionerDemo");
        // 触发acknowledgement机制，否则是fire and forget，可能会引起数据丢失
        // 值为0,1,-1,可以参考
        // http://kafka.apache.org/08/configuration.html
        props.put("request.required.acks", "1");

        org.apache.kafka.clients.producer.Producer producer = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(props);

        // 创建producer
        // 产生并发送消息
        long start = System.currentTimeMillis();
        for (long i = 0; i < events; i++) {

            producer.send(new ProducerRecord<String, String>("my-topic", "my-key-"+Long.toString(i), "my-value-"+Long.toString(i)));
        }
        System.out.println("耗时:" + (System.currentTimeMillis() - start));
        // 关闭producer
        producer.close();
    }
}
