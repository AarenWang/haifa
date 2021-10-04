package org.wrj.haifa.okhttp3;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class HttpUtil {

    public static String httpGetBodyContext(String url){
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .get()//默认就是GET请求，可以不写
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            return call.execute().body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
