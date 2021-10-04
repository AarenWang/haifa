package org.wrj.haifa.java8.time;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;


public class TimezoneTest {

    public static void main(String[] args) {

        ZoneOffset  zoneOffset = ZoneOffset.ofHours(8);
        System.out.println(zoneOffset);
        ZoneOffset  zoneOffset2 = ZoneOffset.from(ZonedDateTime.now());
        System.out.println(zoneOffset2);

        //  抛出异常 Exception in thread "main" java.time.DateTimeException: Unable to obtain ZoneOffset
        //  from TemporalAccessor: 2021-09-07T22:42:34.645 of type java.time.LocalDateTime
        //ZoneOffset  zoneOffset3 = ZoneOffset.from(LocalDateTime.now());
        // System.out.println(zoneOffset3);

        ZoneOffset  zoneOffset4 = ZoneOffset.from(OffsetDateTime.now());
        System.out.println(zoneOffset4);

    }
}
