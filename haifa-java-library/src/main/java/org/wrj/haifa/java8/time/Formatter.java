package org.wrj.haifa.java8.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Formatter {

    public static void main(String[] args) {
        DateTimeFormatter formatter= DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime localDateTime = LocalDateTime.parse("2020-10-03T16:18:09.140Z",formatter);
        System.out.println(localDateTime);
    }
}
