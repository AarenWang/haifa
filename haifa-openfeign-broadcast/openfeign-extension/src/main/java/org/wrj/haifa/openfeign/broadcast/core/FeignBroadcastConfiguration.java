package org.wrj.haifa.openfeign.broadcast.core;

import feign.RequestInterceptor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FeignBroadcastConfiguration {

    @Bean
    public RequestInterceptor feignBroadcastRequestInterceptor() {
        return new FeignBroadcastRequestInterceptor();
    }

    @Bean
    public FeignBroadcastBeanPostProcessor feignBroadcastBeanPostProcessor(
            DiscoveryClient discoveryClient) {
        return new FeignBroadcastBeanPostProcessor(discoveryClient);
    }
}
