package org.wrj.haifa.openfeign.broadcast.core;

import feign.Client;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Replaces the default Feign load-balancer client with broadcast-capable variants.
 */
class FeignBroadcastBeanPostProcessor implements BeanPostProcessor {

    private final DiscoveryClient discoveryClient;

    FeignBroadcastBeanPostProcessor(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (bean instanceof BroadcastClientMarker) {
            return bean;
        }
        if (bean instanceof Client client) {
            return new BroadcastFeignClient(client, discoveryClient);
        }
        return bean;
    }
}
