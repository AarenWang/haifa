package me.wrj.flink13.chapter2;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.AggregateOperator;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.FlatMapOperator;
import org.apache.flink.api.java.operators.UnsortedGrouping;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

public class BatchWordCount {

    public static void main(String[] args) throws Exception {
        System.out.println(System.getenv("PWD"));

        // set up the batch execution environment
        ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();

        // read data from  file
        DataSource<String> lineData = env.readTextFile("haifa-flink13/src/main/resources/words.txt");

        // split up the lines in pairs (2-tuples) containing: (word,1)
        FlatMapOperator<String, Tuple2<String,Long>> wordAndOneTuple1= lineData.flatMap((String line, Collector<Tuple2<String,Long>> out) -> {
                    String[] words = line.split(" ");
                    for(String word : words){
                        out.collect(Tuple2.of(word,1L));
                    }
                }).returns(Types.TUPLE(Types.STRING, Types.LONG));

        // group by the tuple field "0" and sum up tuple field "1"
        UnsortedGrouping<Tuple2<String, Long>> result = wordAndOneTuple1.groupBy(0);
        // group by the tuple field "0" and sum up tuple field "1"
        AggregateOperator<Tuple2<String,Long>> sum = result.sum(1);
        sum.print();

    }
}
