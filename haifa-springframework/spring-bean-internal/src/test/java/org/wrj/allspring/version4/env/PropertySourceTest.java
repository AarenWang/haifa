package org.wrj.allspring.version4.env;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.wrj.allspring.version4.env.profile.AppInfo;

/**
 * Created by wangrenjun on 2017/3/13.
 */

public class PropertySourceTest {

    public static void main(String[] args) {
        // ApplicationContext ctx = new GenericApplicationContext();
        ApplicationContext ctx = new AnnotationConfigApplicationContext("classpath:/spring/env/*.xml");
        Environment env = ctx.getEnvironment();
        boolean containsPath = env.containsProperty("PATH");

        System.out.println("Does my environment contain the 'PATH' property? " + containsPath);
        if (containsPath) {
            System.out.printf("PATH=%s \r\n", env.getProperty("PATH"));
        }


        AppInfo appInfo = (AppInfo) ctx.getBean("appInfo");
        System.out.printf("appname %s, appversion=%s \r\n", appInfo.getAppName(), appInfo.getAppVersion());

    }
}
