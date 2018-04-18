package org.wrj.haifa.log.logback.mybatis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangrenjun on 2018/4/9.
 */
public class MybatisLog {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger("MybatisLog");
        logger.info("Hello MybatisLog Logback.");
    }
}
