package org.wrj.haifa.guava;

import com.google.common.cache.*;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangrenjun on 2017/4/19.
 */
public class GuavaCacheTest {

    public static void main(String[] args) throws Exception {

        Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

        String resultVal = cache.get("jerry", new Callable<String>() {

            public String call() {
                String strProValue = "hello " + "jerry" + "!";
                return strProValue;
            }
        });

        LoadingCache<String, String> cacheBuilder = CacheBuilder.newBuilder()
                .maximumSize(100L)
                .weakKeys()
                .expireAfterAccess(3, TimeUnit.SECONDS)
//                .maximumWeight(50)
//                .weigher(new Weigher<String, String>() {
//
//                    @Override
//                    public int weigh(String key, String value) {
//                        return key.charAt(0);
//                    }
//                })
                .removalListener(new RemovalListener<String, String>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, String> notification) {
                        System.out.println("RemovalListener key=" + notification.getKey()+",value="+notification.getValue());
                        //notification.setValue("new_"+notification.getValue());
                    }
                }).build(new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                String strProValue = "hello " + key + "!";
                return strProValue;
            }
        });


        cacheBuilder.put("1","a");
        cacheBuilder.put("2","a");
        cacheBuilder.put("3","a");
        cacheBuilder.put("4","d");
        cacheBuilder.get("1");
        cacheBuilder.invalidate("2");
        System.out.println(cacheBuilder.get("2"));
        Thread.sleep(4);
        System.out.println(cacheBuilder.get("1"));



    }

}
