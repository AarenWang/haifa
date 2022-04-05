package org.wrj.haifa.springsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"org.wrj.haifa.springsecurity"})
public class SpringSecurityApplication {


    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityApplication.class, args);
    }


}