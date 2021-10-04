package org.wrj.allspring.version4.hook;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by wangrenjun on 2017/3/14.
 */
public class BeanFactoryPostProcessorTest {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring/hook/hook.xml");
        UserModel userModel = applicationContext.getBean(UserModel.class);
        System.out.println(userModel.getCreateTime());

    }
}
