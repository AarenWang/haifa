package org.wrj.haifa.regex;

import com.alibaba.fastjson.JSON;

public class RemoveTraceId {

    static  String logContent = "2021-09-24 10:55:52.926 broker_stats {\"cluster\":\"DefaultCluster\",\"brokerPutNums\":null,\"brokerIp\":\"192.168.94.13:10911\",\"brokerGetNums\":80.48333333333333,\"brokerName\":\"broker-c\"} [\"traceId\":\"6e28a01575ecc2f83932fdbb71fa4587\",\"spanId\":\"323e3ea9c9a57195\"]";;
    public static void main(String[] args) {
        logContent = logContent.replaceFirst("\\[\\\"traceId\\\"\\:\\\"\\w{32}\\\",\\\"spanId\\\":\\\"\\w{16}\\\"\\]","");
        System.out.println(logContent);
        int startIndex =logContent.indexOf("{");
        int endIndex = logContent.lastIndexOf("}");
        if( startIndex != -1 && endIndex != -1 && endIndex > startIndex){
            String json = logContent.substring(startIndex,endIndex+1);
            System.out.println(json);
            JSON.parseObject(json+"223");
        }


    }
}
