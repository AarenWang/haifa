package org.wrj.haifa.modern.typeinference;

public class PatternMatchingInstanceofExample {
    public static void main(String[] args) {
        Object o = "patternMatching";
        if (o instanceof String s) {
            System.out.println("Upper: " + s.toUpperCase());
        } else {
            System.out.println("Not a string");
        }
    }
}
