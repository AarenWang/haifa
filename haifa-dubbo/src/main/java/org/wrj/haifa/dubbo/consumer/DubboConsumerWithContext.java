package org.wrj.haifa.dubbo.consumer;

import com.alibaba.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wrj.haifa.dubbo.api.DubboConstant;

/**
 * Created by wangrenjun on 2018/4/19.
 */
public class DubboConsumerWithContext {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[]{"classpath*:spring-dubbo-consumer.xml"});
        context.start();

        // obtain proxy object for remote invocation
        //TimeService timeService = (TimeService) context.getBean(TimeService.class);
        // execute remote invocation

        RpcContext.getContext();
        RpcContext.getContext().getAttachments().put(DubboConstant.DUBBO_CLIENT_THREAD_NAME,"dubbo-consumer-"+Thread.currentThread().getName());
        System.out.println( RpcContext.getContext().getUrl());;
       //timeService.invokeWithContxtInfo();
        // show the result
        //System.out.println(time);

    }
}
