package org.wrj.haifa.jvmstudy;

import java.util.ArrayList;
import java.util.List;

public class RuntimeConstantPoolOOM {

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        int i =0;
        while (true){
            list.add(String.valueOf(i++).intern());
            while (i % 10_000 == 0){
                System.out.println(i);
            }
        }

    }
}
