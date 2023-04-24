package org.wrj.java8time;

import org.apache.commons.lang3.tuple.Pair;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;

public class Time {

    public static void main(String[] args) {

        Pair<ZonedDateTime,ZonedDateTime> pair = getLastWeekPeriod();
        System.out.println(pair.getLeft());
        System.out.println(pair.getRight());
    }


    static Pair<ZonedDateTime,ZonedDateTime> getLastWeekPeriod() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime lastWeek = now.minusWeeks(1);

        DayOfWeek dayOfWeek = lastWeek.getDayOfWeek();
        DayOfWeek monday = DayOfWeek.MONDAY;
        ZonedDateTime start = lastWeek.with(dayOfWeek.minus(dayOfWeek.getValue() - monday.getValue())).truncatedTo(java.time.temporal.ChronoUnit.DAYS);

        ZonedDateTime end = start.plusDays(7L);
        return Pair.of(start, end);
    }
}
