package org.wrj.allspring.version4.env.profile;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/9/29.
 */
@Profile("default")
@Service
public class DefaultConfigService implements ConfigService{


    public String getJDBCURL() {
        return "default.wrj.me";
    }



}
