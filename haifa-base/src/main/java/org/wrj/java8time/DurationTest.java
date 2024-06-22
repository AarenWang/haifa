package org.wrj.java8time;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class DurationTest {

    public static void main(String[] args) {
        LocalDateTime d1 = LocalDateTime.of(2023,8,6,0,0,0);

        LocalDateTime d2 = LocalDateTime.of(2023,8,7,8,34,32);

        //Period p1 = Period.between(d1,d2);


        Duration du1 = Duration.between(d1,d2);

        System.out.println(du1);
        //System.out.println(du1.get(ChronoUnit.DAYS));
        //System.out.println(du1.get(ChronoUnit.HOURS));
        //System.out.println(du1.get(ChronoUnit.MINUTES));
        System.out.println(du1.get(ChronoUnit.SECONDS));



    }
}
