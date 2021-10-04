package org.wrj.haifa.regex;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


public class JSONExtract {

    static String line = "2021-09-24 10:55:52.926 broker_stats {\"cluster\":\"DefaultCluster\",\"brokerPutNums\":null,\"brokerIp\":\"192.168.94.13:10911\",\"brokerGetNums\":80.48333333333333,\"brokerName\":\"broker-c\"}";



    public static void main(String[] args) {
        Pattern p = Pattern.compile("(?:\"(\\\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\\\\"])*\"(\\s*:)?|\\b(true|false|null)\\b|-?\\d+)");
        System.out.println(p.pattern());

        Matcher m = p.matcher(line);
        //System.out.println(m.matches());
        System.out.println(m.find());
        while (m.find()) {
            if (StringUtils.isNotEmpty(m.group().trim())) {
                System.out.println("group:"+m.group());
            }
        }

        System.out.println(m.groupCount());


    }
}
