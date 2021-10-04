package org.wrj.haifa.java8.time;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ISO8601 {

    public static void main(String[] args) {
        ZonedDateTime now = ZonedDateTime.now();
        System.out.println("DateTimeFormatter.ISO_INSTANT:        "+DateTimeFormatter.ISO_INSTANT.format(now));
        System.out.println("DateTimeFormatter.ISO_LOCAL_DATE_TIME:"+DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(now));
        System.out.println("DateTimeFormatter.ISO_OFFSET_DATE_TIME:"+DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now));
        System.out.println("DateTimeFormatter.ISO_DATE_TIME:       "+DateTimeFormatter.ISO_DATE_TIME.format(now));
        System.out.println("DateTimeFormatter.ISO_ORDINAL_DATE:    "+DateTimeFormatter.ISO_ORDINAL_DATE.format(now));
        System.out.println("DateTimeFormatter.ISO_WEEK_DATE:       "+DateTimeFormatter.ISO_WEEK_DATE.format(now));
        System.out.println("DateTimeFormatter.ISO_ZONED_DATE_TIME: "+DateTimeFormatter.ISO_ZONED_DATE_TIME.format(now));


    }
}
