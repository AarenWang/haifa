package org.wrj.haifa.dubbo.api.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Created by wangrenjun on 2018/4/19.
 */
public class DubboProvider {

    public static void main(String[] args) throws Exception {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] {"classpath*:spring-dubbo-provider.xml"});
        context.start();

        // press any key to exit
        System.in.read();
    }
}
