package org.wrj.haifa.jvm.gc;

import java.util.ArrayList;
import java.util.List;

public class G1Test {

    /**
     * -XX:+UseG1GC
     * -XX:+UseConcMarkSweepGC
     * @param args
     */

    public static void main(String[] args) {

        byte[] one_m = new byte[1024*1024];
        for(int i = 0; i < one_m.length; i++){
            one_m[i] = (i+"").getBytes()[0];
        }
        List<byte[]> byteList = new ArrayList();



        long begin = System.currentTimeMillis();
        int count = 0;
        while (true){
//            byteList.add(bytes);
//            try {
//                Thread.sleep(1L);
//                System.out.println("count="+count++);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            one_m = new byte[1024*1024];
            byteList.add(one_m);
            if(count++ % 1800 == 0){
                byteList.clear();
                System.out.println("count="+count);
            }

        }
    }
}
