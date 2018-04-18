package org.wrj.haifa.retrylog;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wangrenjun on 2017/9/24.
 */
public class Main {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("retrylog.xml");
        MyService myService = applicationContext.getBean(MyService.class);
        myService.addResult(1,2);

    }
}
