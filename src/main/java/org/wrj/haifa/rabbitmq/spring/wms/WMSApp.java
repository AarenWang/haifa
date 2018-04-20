package org.wrj.haifa.rabbitmq.spring.wms;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wangrenjun on 2018/4/20.
 */
public class WMSApp {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-wms.xml");
        applicationContext.start();

    }
}
