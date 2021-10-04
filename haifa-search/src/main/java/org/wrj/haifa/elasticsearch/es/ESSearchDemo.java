package org.wrj.haifa.elasticsearch.es;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ESSearchDemo {

    public static void main(String[] args) {
        String ip = "192.168.3.111";
        int port = 9201;
        String username = "elastic";
        String password = "streamcenter";
        RestHighLevelClient client = null;

        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(ip, port, "http")
                ).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback(

                ) {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                            final CredentialsProvider credentialsProvider =
                                    new BasicCredentialsProvider();
                            credentialsProvider.setCredentials(AuthScope.ANY,
                                    new UsernamePasswordCredentials(username, password));
                            return httpClientBuilder
                                    .setDefaultCredentialsProvider(credentialsProvider);
                        } else {
                            return httpClientBuilder;
                        }
                    }
                }).build());

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        TermsAggregationBuilder builder = new TermsAggregationBuilder("fields.ip", ValueType.STRING);

        AggregationBuilder builder1 = AggregationBuilders.terms("fields.ip").field("fields.ip.keyword");

        searchSourceBuilder.aggregation(builder1);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest = searchRequest.source(searchSourceBuilder);
        searchRequest.indices("sc_logcenter_nginx_gateway_service-2021-07-14");
        searchRequest.searchType(SearchType.DEFAULT);

        try {
            SearchResponse response =  client.search(searchRequest);
            List<Aggregation> aggrs = response.getAggregations().asList();
            Terms terms = response.getAggregations().get("fields.ip");
            List<? extends Terms.Bucket> list = terms.getBuckets();
            list.forEach(e -> {
                System.out.println(e.getKey());
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
