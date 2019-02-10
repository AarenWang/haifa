package org.wrj.haifa.log.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangrenjun on 2017/8/16.
 */
public class LogbackHello {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(LogbackHello.class);
        logger.info("Hello Logback.");
    }
}
