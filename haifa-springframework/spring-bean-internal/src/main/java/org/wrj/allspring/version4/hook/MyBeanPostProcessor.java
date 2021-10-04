package org.wrj.allspring.version4.hook;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/3/14.
 */
@Service
public class MyBeanPostProcessor implements BeanPostProcessor{



    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("postProcessBeforeInitialization "+beanName+" Bean初始化之前");
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("postProcessAfterInitialization. "+beanName +" Bean初始化之后");
        return bean;
    }
}
