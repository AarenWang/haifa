package org.wrj.haifa.hannlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.py.Pinyin;

import java.util.List;

public class DemoPinyin {

    public static void main(String[] args) {
//        String text = "重载不是重任";
        String text = "南京市长江大桥";
        List<Pinyin> pinyinList = HanLP.convertToPinyinList(text);
        System.out.print("原文,");
        for (char c : text.toCharArray())
        {
            System.out.printf("%c,", c);
        }
        System.out.println();

        System.out.print("拼音（数字音调）,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin);
        }
        System.out.println();

        System.out.print("拼音（符号音调）,");
        for (Pinyin pinyin : pinyinList)
        {
            System.out.printf("%s,", pinyin.getPinyinWithToneMark());
        }
        System.out.println();
    }
}
