package org.wrj.java8time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class LocalDateTimeTest {

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
        System.out.println(LocalDateTime.now(ZoneOffset.UTC));


        LocalDateTime ldt = LocalDateTime.now();
        ZonedDateTime ldtZoned = ldt.atZone(ZoneId.systemDefault());
        ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of("UTC"));
        System.out.println(utcZoned.toLocalTime());

    }
}
