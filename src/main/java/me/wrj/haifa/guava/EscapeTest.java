package me.wrj.haifa.guava;

import com.google.common.html.HtmlEscapers;

/**
 * Created by wangrenjun on 2017/4/19.
 */
public class EscapeTest {

    public static void main(String[] args) {
        String html = " a > b && wangrenjun@sina.com";
        html = HtmlEscapers.htmlEscaper().escape(html);
        System.out.println(html);
    }
}
