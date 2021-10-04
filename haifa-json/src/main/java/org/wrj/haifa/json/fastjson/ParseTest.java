package org.wrj.haifa.json.fastjson;

import com.alibaba.fastjson.JSON;

public class ParseTest {

    public static void main(String[] args) {
        String text = "556 23423 {}";
        JSON.parseObject(text);
    }
}
