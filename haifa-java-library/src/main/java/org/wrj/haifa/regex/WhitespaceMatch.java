package org.wrj.haifa.regex;

public class WhitespaceMatch {

    public static void main(String[] args) {
        String keyword="2021052133523   中国";
        System.out.println(keyword.contains(" "));
        String[] words = keyword.split("\\s+");

        System.out.println(words.length);


    }
}
