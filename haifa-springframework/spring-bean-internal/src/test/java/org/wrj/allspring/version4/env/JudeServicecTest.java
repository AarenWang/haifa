package org.wrj.allspring.version4.env;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JudeServicecTest {

    public static void main(String[] args) {
        //AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext("spring/env/*.xml");
        //ctx.register(JudeService.class);
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:spring/env/env.xml");
        JudeService judeService = ctx.getBean(JudeService.class);
        System.out.println(judeService.getJudeHost());
    }
}
