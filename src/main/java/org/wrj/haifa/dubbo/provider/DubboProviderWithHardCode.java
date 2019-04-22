package org.wrj.haifa.dubbo.provider;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.wrj.haifa.dubbo.api.TimeService;
import sun.tools.java.ClassPath;

import java.io.IOException;

public class DubboProviderWithHardCode {

    public static void main(String[] args) throws IOException {
        // 普通编码配置方式
        ApplicationConfig application = new ApplicationConfig();
        application.setName("demo-consumer");

        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181");

        ServiceConfig<org.wrj.haifa.dubbo.api.TimeService> timeServiceServiceConfig = new ServiceConfig<>();

        ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[]{"classpath*:spring-dubbo-provider.xml"});




        System.in.read();

    }
}
