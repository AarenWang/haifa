package org.wrj.allspring.version4.env.profile;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/3/13.
 */
@Profile("prod")
@Service
public class ProdConfigServiceImpl implements ConfigService{
    public String getJDBCURL() {
        return "prod.wrj.me";
    }
}
