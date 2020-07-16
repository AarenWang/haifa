package org.wrj.haifa.java8;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;

public class CollectorTest {

    public static void main(String[] args) {

        List<Integer>  list = Lists.newArrayList(6,3,5,7,2,5,9,8);
        list.sort(Comparator.comparing(Integer::intValue).reversed());
        System.out.println(StringUtils.join(list,","));
    }
}
