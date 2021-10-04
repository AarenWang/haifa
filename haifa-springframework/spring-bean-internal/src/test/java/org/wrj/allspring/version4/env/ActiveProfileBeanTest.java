package org.wrj.allspring.version4.env;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.wrj.allspring.version4.env.profile.ConfigService;
import org.wrj.allspring.version4.env.profile.DefaultConfigService;
import org.wrj.allspring.version4.env.profile.DevConfigSewrviceImpl;
import org.wrj.allspring.version4.env.profile.ProdConfigServiceImpl;

/**
 * Created by wangrenjun on 2017/3/13.
 */
public class ActiveProfileBeanTest {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext("spring/env/*.xml");

        //ctx.getEnvironment().setActiveProfiles("dev");
        ctx.register(DevConfigSewrviceImpl.class, ProdConfigServiceImpl.class, DefaultConfigService.class);
        //如果激活的是dev profile 但是注册的是ProdConfigServiceImpl 将报NoSuchBeanDefinitionException
        //ctx.register( ProdConfigServiceImpl.class);
        ConfigService configService = ctx.getBean(ConfigService.class);
        System.out.println(configService.getJDBCURL());



    }
}
