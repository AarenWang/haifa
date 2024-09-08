package me.wrj.flink.fraud;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import me.wrj.flink.fraud.sink.AlertSink;
import me.wrj.flink.fraud.entity.Alert;
import me.wrj.flink.fraud.entity.Transaction;
import me.wrj.flink.fraud.source.TransactionSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Skeleton code for the datastream walkthrough
 */
public class FraudDetectionJob {
    public static void main(String[] args) throws Exception {
        //StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Map<String, String> map  = new HashMap<>();
        map.put("rest.flamegraph.enabled","true");
        Configuration conf = Configuration.fromMap(map);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

        DataStream<Transaction> transactions = env
                .addSource(new TransactionSource())
                .name("transactions");

        DataStream<Alert> alerts = transactions
                .keyBy(Transaction::getAccountId)
                .process(new FraudDetector())
                .name("fraud-detector");

        alerts
                .addSink(new AlertSink())
                .name("send-alerts");

        env.execute("Fraud Detection");
    }
}
