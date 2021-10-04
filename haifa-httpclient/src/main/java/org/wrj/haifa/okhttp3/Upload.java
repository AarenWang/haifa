package org.wrj.haifa.okhttp3;

import okhttp3.*;

import java.io.File;

public class Upload {

    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file","logstash-7.7.0.tar.gz",
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File("/Users/wangrenjun/Downloads/logstash-7.7.0.tar.gz")))
                .addFormDataPart("orginFileName","logstash-7.7.0.tar.gz")
                .addFormDataPart("bizCode","logcenter_web")
                .addFormDataPart("bizUnuiqId","2021928_184224_357")
                .addFormDataPart("sign","64ce34df7d511f369b286b180a88e78a")
                .build();
        Request request = new Request.Builder()
                .url("http://kano-control.guahao.cn/uploadfile?orginFileName=logstash-7.7.0.tar.gz&bizCode=logcenter_web&bizUnuiqId=2021928_184224_357&sign=64ce34df7d511f369b286b180a88e78a")
                .method("POST", body)
                .build();
        try{
            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
