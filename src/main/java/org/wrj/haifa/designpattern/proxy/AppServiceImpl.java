package org.wrj.haifa.designpattern.proxy;

/**
 * Created by wangrenjun on 2017/9/24.
 */
public class AppServiceImpl  implements AppService{

    public boolean createApp(String name) {
        System.out.println("App["+name+"] has been created.");
        return true;
    }
}
