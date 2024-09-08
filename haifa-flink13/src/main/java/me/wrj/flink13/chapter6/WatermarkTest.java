package me.wrj.flink13.chapter6;

import me.wrj.flink13.ClickSource;
import me.wrj.flink13.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

public class WatermarkTest {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.addSource(new ClickSource())
                // 插入水位线的逻辑
                .assignTimestampsAndWatermarks(
                        // 针对乱序流插入水位线，延迟时间设置为 5s
                        WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(5)).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                            // 抽取时间戳的逻辑
                            @Override
                            public long extractTimestamp(Event element, long recordTimestamp) {
                                return element.timestamp;
                            }
                        })).print();

        env.execute();

    }

}
