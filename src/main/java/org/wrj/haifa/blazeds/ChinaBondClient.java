package org.wrj.haifa.blazeds;

import flex.messaging.io.amf.client.AMFConnection;

/**
 * Created by wangrenjun on 2017/9/11.
 */
public class ChinaBondClient {

    public static void main(String[] args) {
        AMFConnection amfConnection = new AMFConnection();
        try {
            String url = "http://indices.chinabond.com.cn/cbweb-mn/messagebroker/amf";
            amfConnection.connect(url);
            Object result = amfConnection.call("multiIndexQuery.getMultiIndex", "charles@example.com");
            //Object result = amfConnection.call("my-amf", new Object());
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            amfConnection.close();
        }

    }
}
