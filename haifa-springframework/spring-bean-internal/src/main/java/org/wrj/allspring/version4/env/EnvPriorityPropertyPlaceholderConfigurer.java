package org.wrj.allspring.version4.env;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class EnvPriorityPropertyPlaceholderConfigurer  extends PropertyPlaceholderConfigurer {

    //是否开启环境优先
    private  boolean  enableEnvPriority = false;


    // 缓存所有的属性配置
    private Properties properties;

    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public boolean isEnableEnvPriority() {
        return enableEnvPriority;
    }

    public void setEnableEnvPriority(boolean enableEnvPriority) {
        this.enableEnvPriority = enableEnvPriority;
    }


    protected Properties mergeProperties() throws IOException {
        Properties mergeProperties = super.mergeProperties();
        if(!enableEnvPriority){
            return mergeProperties;
        }
        else {
            this.properties = new Properties();
            Map<String, String> envMap = System.getenv();
            String envProfile = envMap.get(EnvConstant.ENV_PROFILE);

            //读取不到该环境变量 不做处理
            if(StringUtils.isEmpty(envProfile)){
                return mergeProperties;
            }

            Set<Map.Entry<Object, Object>> es = mergeProperties.entrySet();
            for (Map.Entry<Object, Object> entry : es) {
                String key = (String) entry.getKey();
                String envKey = envProfile+"."+key;
                if(mergeProperties.containsKey(envKey)){
                    mergeProperties.put(key,mergeProperties.getProperty(envKey));
                }
            }
            return mergeProperties;
        }
    }
}
