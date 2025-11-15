package org.wrj.haifa.ai.spring.toolcalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class GeoToolCallingApplication {

    private static final Logger log = LoggerFactory.getLogger(GeoToolCallingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GeoToolCallingApplication.class, args);
        log.info("GeoToolCallingApplication started");
    }
}
