package org.wrj.haifa.elasticsearch;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import java.util.*;

public class BizExceptionDataMock {

    public static void main(String[] args) throws IOException {


        if(args.length < 4) {
            throw  new IllegalArgumentException("参数不正确");
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String userName = args[2];
        String password = args[3];

        RestHighLevelClient  client = getClient(host,port,userName,password);

        //List<BizExceptionAndErrorPO>  poList = buildBizExceptionPOList();
        //mockBizExceptionData(poList, client);
        List<BizExceptionAndErrorPO>  poList =buildSystemErrorPOList();
        mockBizExceptionData(poList, client);

    }

    private static void mockBizExceptionData(List<BizExceptionAndErrorPO> poList, RestHighLevelClient client) throws IOException {
        for(BizExceptionAndErrorPO  po : poList) {
            String indexName = "realtime:agg:service:error:exp"+"-"+new Timestamp(po.getTimestamp()).toLocalDateTime().toLocalDate().toString();
            IndexRequest indexRequest = new IndexRequest(indexName,"realtime:agg:service:error:exp");
            Map<String,Object> map = new HashMap<>();
            map.put("serviceName",po.getServiceName());
            map.put("timestamp",po.getTimestamp());
            map.put("count",po.getCount());
            map.put("exceptionOrError",po.getExceptionOrError());
            map.put("bizExceptionCode",po.getBizExceptionCode());
            map.put("bizExceptionClass",po.getBizExceptionClass());
            map.put("errorCode",po.getErrorCode());
            map.put("errorClass",po.getErrorClass());
            map.put("message",po.getMessage());

            indexRequest.source(map);
            client.index(indexRequest);
        }
    }

    private static  List<BizExceptionAndErrorPO> buildBizExceptionPOList() {
        String[] bizExceptionClass = new String[]{"com.guahao.gtrace.exception.UserNotExistException","com.guahao.gtrace.exception.ApplicationStateIllegalException"};
        String[] bizCode = new String[]{"UserNotExist","ApplicationStateIllegal"};
        String[] message = new String[]{"用户不存在","应用状态异常"};
        long end = System.currentTimeMillis();
        long begin = end - 3600 * 24 * 1000L;
        Random random = new Random(5);
        List<BizExceptionAndErrorPO> poList = new ArrayList<>();
        while (begin <= end) {
            BizExceptionAndErrorPO  po = new BizExceptionAndErrorPO();
            int index = random.nextInt(2);
            po.setBizExceptionClass(bizExceptionClass[index]);
            po.setBizExceptionCode(bizCode[index]);
            po.setExceptionOrError(1);
            po.setServiceName("echat-cs-service");
            Timestamp timestamp = new Timestamp(begin);
            //Instant instant = timestamp.toLocalDateTime().toInstant(ZoneOffset.of(ZoneOffset.systemDefault().normalized().getId()));
            Instant instant = timestamp.toLocalDateTime().toInstant(ZoneOffset.of("+8"));
            long epollMillSecond = instant.truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
            po.setTimestamp(epollMillSecond);
            po.setMessage(message[1]);
            po.setCount(random.nextInt(5));
            po.setErrorClass(null);
            po.setErrorCode(null);

            begin = begin + 60 * 1000L;

            poList.add(po);
            System.out.println(po);
        }
        return poList;
    }


    private static  List<BizExceptionAndErrorPO> buildSystemErrorPOList() {
        String[] systemExceptionClass = new String[]{"org.apache.dubbo.rpc.RpcException"
                ,"java.lang.IllegalStateException"};
        String[] errorCode = new String[]{"RpcException","IllegalStateException"};
        String[] message = new String[]{"Failed to invoke the method $echo in the service com.greenline.kano.service.share.file.KanoFileService"
                ,"failed to req API:/nacos/v1/ns/instance after all servers([nacos.guahao-test.com:80, nacos.guahao-test.com:80, nacos.guahao-test.com:80]) tried: failed to req API:nacos.guahao-test.com:80/nacos/v1/ns/instance. code:500 msg: java.net.SocketTimeoutException: Read timed out"};
        long end = System.currentTimeMillis();
        long begin = end - 3600 * 24 * 1000L;
        Random random = new Random(5);
        List<BizExceptionAndErrorPO> poList = new ArrayList<>();
        while (begin <= end) {
            BizExceptionAndErrorPO  po = new BizExceptionAndErrorPO();
            int index = random.nextInt(2);
            po.setBizExceptionClass(null);
            po.setBizExceptionCode(null);
            po.setExceptionOrError(2);
            po.setServiceName("echat-cs-service");
            Timestamp timestamp = new Timestamp(begin);
            //Instant instant = timestamp.toLocalDateTime().toInstant(ZoneOffset.of(ZoneOffset.systemDefault().normalized().getId()));
            Instant instant = timestamp.toLocalDateTime().toInstant(ZoneOffset.of("+8"));
            long epollMillSecond = instant.truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
            po.setTimestamp(epollMillSecond);
            po.setMessage(message[index]);
            po.setCount(random.nextInt(5));
            po.setErrorClass(systemExceptionClass[index]);
            po.setErrorCode(errorCode[index]);

            begin = begin + 60 * 1000L;

            poList.add(po);
            System.out.println(po);
        }
        return poList;
    }


    private static RestHighLevelClient getClient(String host, int port, String userName, String password) {

        final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(userName, password));

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, "http")
                ).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback(

                ) {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider);
                    }
                }).build());

        return client;
    }
}
