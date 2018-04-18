package org.wrj.haifa.retrylog;

import org.springframework.stereotype.Service;

/**
 * Created by wangrenjun on 2017/9/24.
 */
@Service
public class MyService {

    public  Integer service1(Integer a, Integer b){
        throw  new IllegalArgumentException("every argument is Illegal");
    }


    public  Integer addResult(Integer a, Integer b){
        return  a + b;
    }
}
