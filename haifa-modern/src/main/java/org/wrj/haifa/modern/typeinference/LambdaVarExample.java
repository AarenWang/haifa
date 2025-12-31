package org.wrj.haifa.modern.typeinference;

import java.util.function.BiFunction;

public class LambdaVarExample {
    public static void main(String[] args) {
        BiFunction<Integer, Integer, Integer> add = (var a, var b) -> a + b;
        System.out.println("3+4=" + add.apply(3, 4));

        java.util.function.Consumer<java.util.List<String>> printer = (var list) -> list.forEach(System.out::println);
        printer.accept(java.util.List.of("alpha", "beta"));
    }
}
