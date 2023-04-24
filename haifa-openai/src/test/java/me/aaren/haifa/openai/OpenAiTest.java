package me.aaren.haifa.openai;


import okhttp3.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class OpenAiTest {

    private static final String OPEN_AI_COMPLETIONS = "https://api.openai.com/v1/completions";

    private static final  String SECRET_KEY ="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";

    @Test
    public void testCompletions() throws IOException {
        String postBody = "{\"model\": \"text-davinci-003\",\"prompt\": \"兔子洞是什么意思\", \"temperature\": 0, \"max_tokens\": 1000}";
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                postBody,
                MediaType.parse("application/json")
        );


        Request request = new Request.Builder()
                .url(OPEN_AI_COMPLETIONS)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization","Bearer "+SECRET_KEY)
                .post(body)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        Assertions.assertTrue(response.code() == 200);
        System.out.println(response.body().string());

    }

}
