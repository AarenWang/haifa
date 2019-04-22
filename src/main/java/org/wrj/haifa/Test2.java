package org.wrj.haifa;

import org.apache.tomcat.jni.Local;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Test2 {

    /**
     *
     * @param repeatDays  一周里 星期几会运行
     * @param repeatTime  运行时间点
     * @param compareTime 判断的其实时间点
     * @return
     */
    public LocalDateTime getNextRunTime(List<DayOfWeek> repeatDays, LocalTime repeatTime, LocalDateTime compareTime) {
        if (repeatDays == null || repeatDays.size() == 0 || repeatTime == null || compareTime == null) {
            return null;
        }

        for (int i = 0; i < repeatDays.size(); i++) {
            DayOfWeek dayOfWeek = repeatDays.get(i);
            //当天是运行日
            if (compareTime.getDayOfWeek() == dayOfWeek) {
                //当天会重复运行， 且时间还没到
                if (compareTime.toLocalTime().isBefore(repeatTime)) {
                    return compareTime.withHour(repeatTime.getHour()).withMinute(repeatTime.getMinute()).withSecond(repeatTime.getSecond());
                }
                //时间过了
                else {
                    //不是最后一天
                    if (i < repeatDays.size() - 1) {
                        return compareTime.plusDays(repeatDays.get(i + 1).getValue() - compareTime.getDayOfWeek().getValue()).withHour(repeatTime.getHour()).withMinute(repeatTime.getMinute()).withSecond(repeatTime.getSecond());
                    }
                    //下周再运行
                    else {
                        return compareTime.plusDays(7 - (compareTime.getDayOfWeek().getValue() - repeatDays.get(0).getValue())).withHour(repeatTime.getHour()).withMinute(repeatTime.getMinute()).withSecond(repeatTime.getSecond());
                    }
                }
            }
            if(compareTime.getDayOfWeek().getValue() > dayOfWeek.getValue()) {
                //不是最后一日
                if(i < repeatDays.size() -1) {
                    //比下一重复日要小  那就下一重复日
                    if(compareTime.getDayOfWeek().getValue()  < repeatDays.get(i+1).getValue()) {
                        return compareTime.plusDays(repeatDays.get(i + 1).getValue() - compareTime.getDayOfWeek().getValue()).withHour(repeatTime.getHour()).withMinute(repeatTime.getMinute()).withSecond(repeatTime.getSecond());
                    }
                    //else {} 这里走当天重复工作日
                }
                //下一周第一个重复日
                else {
                    compareTime.plusDays(7 - (compareTime.getDayOfWeek().getValue() - repeatDays.get(0).getValue())).withHour(repeatTime.getHour()).withMinute(repeatTime.getMinute()).withSecond(repeatTime.getSecond());
                }
            }
        }

        return null;
    }



}


