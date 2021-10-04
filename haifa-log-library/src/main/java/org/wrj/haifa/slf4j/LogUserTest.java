package org.wrj.haifa.log.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangrenjun on 2017/7/19.
 */
public class LogUserTest {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(LogUserTest.class);
        System.out.println("logger.getName()"+logger.getName());
        logger.info("Hello World");
    }
}
