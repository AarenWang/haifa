package org.wrj.allspring.version4.env.profile;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/9/29.
 */
@Component
@Service("appInfo")
public class AppInfo implements InitializingBean{

    @Value("app.name")
    private  String appName;

    @Value("app.version")
    private  String appVersion;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet");
    }
}
