package org.wrj.allspring.version4.hook;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

/**
 * Created by wangrenjun on 2017/3/14.
 */
@Service
public class BaseModelBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("BaseModelBeanFactoryPostProcessor.postProcessBeanFactory");
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String def : beanDefinitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(def);
            MutablePropertyValues mpv = beanDefinition.getPropertyValues();

            if (beanDefinition.getBeanClassName().endsWith("Model")) {
                beanDefinition.getPropertyValues().add("createTime", new Timestamp(System.currentTimeMillis()));
                beanDefinition.getPropertyValues().add("updateTime", new Timestamp(System.currentTimeMillis()));
            }

        }

    }
}
