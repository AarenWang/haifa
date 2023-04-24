package org.wrj.haifa.dubbo.api.consumer;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wrj.haifa.dubbo.api.TimeService;

import java.sql.Timestamp;

/**
 * Created by wangrenjun on 2018/4/19.
 */
public class DubboConsumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[]{"classpath*:spring-dubbo-consumer.xml"});
        context.start();
        // obtain proxy object for remote invocation
        TimeService timeService = (TimeService) context.getBean(TimeService.class);
        // execute remote invocation
        Timestamp time = timeService.getCurrentTime();
        // show the result
        //System.out.println(time);

        timeService.getSomething();
    }
}
