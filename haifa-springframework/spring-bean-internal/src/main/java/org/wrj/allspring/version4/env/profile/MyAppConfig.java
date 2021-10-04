package org.wrj.allspring.version4.env.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Created by wangrenjun on 2017/9/29.
 */
@Configuration
@PropertySource("classpath:spring/env/env.properties")
public class MyAppConfig {

    @Autowired
    private Environment env;

    @Bean
    public AppInfo config() {
        AppInfo appInfo = new AppInfo();
        appInfo.setAppName(env.getProperty("app.name"));
        appInfo.setAppVersion(env.getProperty("app.version"));
        return appInfo;
    }




}
