package org.wrj.haifa.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegextTest2 {

    public static void main(String[] args) {
        String[] strs = {"/data/logs/membership-service-stable-57574474d9-rr64h",
        "/data/logs/zeus-service-qa1-6d4fcb5cb6-pmzn4",
        "/data/logs/zeus-service-dev1-6d4fcb5cb6-pmzn4",
        "/data/logs/zeus-my-service-dev1-6d4fcb5cb6-pmzn4"};

        Pattern p1 = Pattern.compile("-stable-");
        Pattern p2 = Pattern.compile("-dev\\d{1,2}-");
        Pattern p3 = Pattern.compile("-test\\d{1,2}-");
        for(String str : strs) {

            Matcher match = p1.matcher(str);
            if(match.find()){
                System.out.println("p1 match "+str);
            }

            match = p2.matcher(str);
            if(match.find()){
                System.out.println("p2 match "+str);
            }

            match = p3.matcher(str);
            if(match.find()){
                System.out.println("p3 match "+str);
            }
        }
    }
}
