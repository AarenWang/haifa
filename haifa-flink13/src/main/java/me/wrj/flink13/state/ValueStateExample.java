package me.wrj.flink13.state;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

public class ValueStateExample<I extends Number> {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.fromElements(Tuple2.of("user1", 1), Tuple2.of("user2", 1), Tuple2.of("user1", 1), Tuple2.of("user2", 1))
                .keyBy(0)
                .flatMap(new CountWithKeyedState())
                .print();
        env.execute("Flink ValueState example");
    }



    public static class CountWithKeyedState extends RichFlatMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>> {

        private transient ValueState<Integer> countState;

        @Override
        public void open(Configuration parameters) throws Exception {
            ValueStateDescriptor<Integer> descriptor =
                    new ValueStateDescriptor<>("countState", Integer.class, 0);
            countState = getRuntimeContext().getState(descriptor);
        }

        @Override
        public void flatMap(Tuple2<String, Integer> value, Collector<Tuple2<String, Integer>> out) throws Exception {
            Integer currentCount = countState.value();
            currentCount += value.f1;
            countState.update(currentCount);
            out.collect(Tuple2.of(value.f0, currentCount));
        }


    }

}
