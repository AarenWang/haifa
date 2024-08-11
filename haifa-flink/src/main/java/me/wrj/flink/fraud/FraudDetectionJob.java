package me.wrj.flink.fraud;

import me.wrj.flink.fraud.source.TransactionSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import javax.security.auth.login.AppConfigurationEntry;


public class FraudDetectionJob {
    public static void main(String[] args) throws Exception {
//        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Configuration configuration = new Configuration();
        configuration.setInteger("rest.port", 8081);


        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(configuration);

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