package org.wrj.haifa.log.log4j;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

public class Log4jDemo {

    public static void main(String[] args) {
        //LogManager.DEFAULT_CONFIGURATION_FILE = "/Users/wangrenjun/git/haifa/src/main/resources/log4j.properties";
        BasicConfigurator.configure();


        Logger logger = Logger.getLogger("dailyFile");
        Logger rsyslog = Logger.getLogger("SYSLOG");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (true){

                    String str = "This io a loop i=8122987 Thread-3  EnableGconfigConfiguration SpringApplication LoggerFactory.getLogger(LocalLogcenterWebApplication.class)   Unregistering JMX-exposed beans on  324432432   EnableGconfigConfiguration SpringApplication LoggerFactory";
                    //logger.error(str);
                    rsyslog.error(str);

                    i++;
                    if(i % 10 == 0){
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
