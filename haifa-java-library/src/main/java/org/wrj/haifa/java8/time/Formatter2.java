package org.wrj.haifa.java8.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class Formatter2 {

    public static void main(String[] args) {
        DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");
        DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        String source = "2020-12-22 23:14:03 333";

        TemporalAccessor date = sourceFormatter.parse(source);
        //String targetDate = targetSDF.format(date);
        String targetDate = targetFormatter.format(date);
        System.out.println(targetDate);
    }
}
