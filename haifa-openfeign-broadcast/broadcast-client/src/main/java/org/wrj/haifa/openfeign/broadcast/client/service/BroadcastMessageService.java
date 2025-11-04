package org.wrj.haifa.openfeign.broadcast.client.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.wrj.haifa.openfeign.broadcast.client.feign.BroadcastMessageClient;
import org.wrj.haifa.openfeign.broadcast.model.BroadcastResult;
import org.wrj.haifa.openfeign.broadcast.shared.BroadcastMessageRequest;
import org.wrj.haifa.openfeign.broadcast.shared.MessageRecord;

@Service
public class BroadcastMessageService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastMessageService.class);

    private final BroadcastMessageClient feignClient;
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final String applicationName;

    public BroadcastMessageService(
            BroadcastMessageClient feignClient,
            DiscoveryClient discoveryClient,
            RestTemplate restTemplate,
            @Value("${spring.application.name}") String applicationName) {
        this.feignClient = feignClient;
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
        this.applicationName = applicationName;
    }

    public BroadcastResult broadcast(String message) {
        BroadcastMessageRequest request = new BroadcastMessageRequest(applicationName, message);
        BroadcastResult result = feignClient.broadcast(request);
        log.info("Broadcast message '{}' to {} instances", message, result.instances().size());
        return result;
    }

    public Map<String, List<MessageRecord>> fetchProviderState(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        return instances.stream()
                .collect(Collectors.toMap(this::instanceKey, this::fetchMessages));
    }

    private String instanceKey(ServiceInstance instance) {
        String id = instance.getInstanceId();
        if (id != null && !id.isEmpty()) {
            return id;
        }
        return instance.getHost() + ":" + instance.getPort();
    }

    private List<MessageRecord> fetchMessages(ServiceInstance instance) {
        URI uri = instance.getUri();
        URI endpoint = uri.resolve("/api/messages");
        MessageRecord[] response = restTemplate.getForObject(endpoint, MessageRecord[].class);
        return response == null ? List.of() : List.copyOf(Arrays.asList(response));
    }
}
