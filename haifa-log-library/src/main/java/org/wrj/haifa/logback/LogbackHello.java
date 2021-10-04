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


        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (true){

                    String str = "This io a loop i=8122987 Thread-3  EnableGconfigConfiguration SpringApplication LoggerFactory.getLogger(LocalLogcenterWebApplication.class)   Unregistering JMX-exposed beans on  324432432   EnableGconfigConfiguration SpringApplication LoggerFactory";
                    //logger.error(str);
                    logger.error(str);

                    i++;
                    if(i % 100 == 0){
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
}
