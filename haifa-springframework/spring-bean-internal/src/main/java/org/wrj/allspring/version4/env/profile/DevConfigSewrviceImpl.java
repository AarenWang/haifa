package org.wrj.allspring.version4.env.profile;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/3/13.
 */
@Profile("dev")
@Service
public class DevConfigSewrviceImpl implements  ConfigService{


    public String getJDBCURL() {
        return "dev.wrj.me";
    }


}
