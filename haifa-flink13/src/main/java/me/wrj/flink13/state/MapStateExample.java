package me.wrj.flink13.state;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * 使用MapState 统计单词出现次数
 */
public class MapStateExample {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.socketTextStream("localhost", 9999)
                .flatMap(new Tokenizer())
                .keyBy(value -> value.f0)
                .flatMap(new RichFlatMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private transient MapState<String, Integer> wordState;
                    @Override
                    public void open(Configuration parameters){

                        MapStateDescriptor<String, Integer> descriptor =
                                new MapStateDescriptor<>("wordCount", String.class, Integer.class);
                        wordState = getRuntimeContext().getMapState(descriptor);
                    }
                    @Override
                    public void flatMap(Tuple2<String, Integer> value, Collector<Tuple2<String, Integer>> out) throws Exception {
                        Integer count = wordState.get(value.f0);
                        if (count == null) {
                            count = 0;
                        }
                        count += value.f1;
                        wordState.put(value.f0, count);
                        out.collect(Tuple2.of(value.f0, count));
                    }
                })
                .print();
        env.execute("Word Count with MapState");
    }

    public static final class Tokenizer extends RichFlatMapFunction<String, Tuple2<String, Integer>> {

        @Override
        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
            String[] words = value.toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.length() > 0) {
                    out.collect(new Tuple2<>(word, 1));
                }
            }
        }
    }

}
