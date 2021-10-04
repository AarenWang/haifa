package org.wrj.haifa.elasticsearch;


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;



public class ESDemo {

    public static void main(String[] args) throws IOException {
        if(args.length < 4) {
            throw  new IllegalArgumentException("参数不正确");
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String userName = args[2];
        String password = args[3];

        RestHighLevelClient  client = getClient(host,port,userName,password);
        String[]  indices= new String[]{"filebeat-2020.10.03"};
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.searchType(SearchType.DEFAULT);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder timestampQB  = QueryBuilders.rangeQuery("@timestamp").from(System.currentTimeMillis() - 24 * 3600 * 1000 ).to(System.currentTimeMillis());
        QueryBuilder  ipQB = QueryBuilders.termQuery("fields.ip","192.168.4.6");
        QueryBuilder  messageQB = QueryBuilders.matchQuery("message", "com.apple.xpc.launchd");
        searchSourceBuilder.query(QueryBuilders.boolQuery().must(timestampQB).must(ipQB).must(messageQB));


        searchRequest.indices(indices);
        searchRequest = searchRequest.source(searchSourceBuilder);


        client.searchAsync(searchRequest, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                System.out.println("totalHits="+searchResponse.getHits().totalHits);
                System.out.println("searchResponse.getHits().getHits().length="+searchResponse.getHits().getHits().length);
            }

            @Override
            public void onFailure(Exception e) {

                System.out.println(e);
            }
        });




    }

    private static RestHighLevelClient getClient(String host,int port,String userName,String password) {

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
