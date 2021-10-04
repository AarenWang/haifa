package org.wrj.haifa.guava;

import com.google.common.base.Stopwatch;

import java.time.Duration;

public class StopwatchTest {


    public static void main(String[] args) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            Thread.sleep(3245);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Duration duration = stopwatch.elapsed();
        System.out.println("duration="+duration);

    }
}
