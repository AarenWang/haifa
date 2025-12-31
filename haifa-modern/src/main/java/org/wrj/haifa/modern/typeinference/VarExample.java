package org.wrj.haifa.modern.typeinference;

import java.util.List;
import java.util.Map;

public class VarExample {
    public static void main(String[] args) {
        var x = 42; // int
        var s = "hello"; // String

        var list = List.of(1, 2, 3);
        for (var v : list) {
            System.out.print(v + " ");
        }
        System.out.println();

        var map = Map.of("a", 1, "b", 2);
        System.out.println("x=" + x + ", s=" + s + ", map=" + map);
    }
}
