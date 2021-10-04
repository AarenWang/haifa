package org.wrj.allspring.version4.env;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JudeService {

    @Autowired
    private  Environment env;

    private  String judeHost;

    public String getJudeHost() {
        //return  env.getProperty(EnvConstant.ENV_PROFILE+"."+"jude.host",judeHost);
        String envProfile = env.getProperty(EnvConstant.ENV_PROFILE);
        String envJudeHost = env.getProperty(envProfile+"."+"jude.host");
        if(StringUtils.isNotEmpty(envJudeHost)){
            return envJudeHost;
        }
        return judeHost;
    }

    public void setJudeHost(String judeHost) {
        this.judeHost = judeHost;
    }


}
