package me.wrj.concurrent.app.concurrentcontainer;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentContainer {

    public static void main(String[] args) {
        ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>(16);
    }

}
