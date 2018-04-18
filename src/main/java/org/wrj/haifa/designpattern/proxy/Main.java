package org.wrj.haifa.designpattern.proxy;

import java.lang.reflect.Proxy;

/**
 * Created by wangrenjun on 2017/9/24.
 */
public class Main {

    public static void main(String[] args) {
        AppService target = new AppServiceImpl();// 生成目标对象
        // 接下来创建代理对象
        AppService proxy = (AppService) Proxy.newProxyInstance(target.getClass().getClassLoader(),
                                                               target.getClass().getInterfaces(),
                                                               new LoggerInterceptor(target));
        proxy.createApp("Kevin Test");
    }
}
