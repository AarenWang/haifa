package org.wrj.haifa;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Test {

    public static void main(String[] args) {
        Instant now = Instant.now();
        System.out.println("now.getEpochSecond="+now.getEpochSecond());

        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println("localDateTime.getLong="+localDateTime.toEpochSecond(ZoneOffset.of("+8")));

        List<Integer> list = new ArrayList();
        list.add(1);
        list.add(2);
        list.add(3);

        list = list.stream().filter(e -> e %2 ==0).collect(Collectors.toList());
        System.out.println(list.size());
    }
}
