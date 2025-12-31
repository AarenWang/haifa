package org.wrj.haifa.modern.typeinference;

import java.util.ArrayList;

public class GenericInferenceExample {
    public static <T> T identity(T t) {
        return t;
    }

    public static void main(String[] args) {
        var i = identity(123); // 推断为 Integer
        System.out.println("identity returned: " + i + " (" + i.getClass().getName() + ")");

        var list = new ArrayList<String>(); // 钻石操作和构造器推断
        list.add("x");
        System.out.println(list);
    }
}
