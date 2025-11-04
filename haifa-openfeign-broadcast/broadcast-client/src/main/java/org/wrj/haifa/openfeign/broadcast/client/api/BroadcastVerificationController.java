package org.wrj.haifa.openfeign.broadcast.client.api;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wrj.haifa.openfeign.broadcast.client.service.BroadcastMessageService;
import org.wrj.haifa.openfeign.broadcast.model.BroadcastResult;
import org.wrj.haifa.openfeign.broadcast.shared.MessageRecord;

@RestController
@RequestMapping("/api/broadcast")
public class BroadcastVerificationController {

    private final BroadcastMessageService messageService;
    private final DiscoveryClient discoveryClient;

    public BroadcastVerificationController(
            BroadcastMessageService messageService,
            DiscoveryClient discoveryClient) {
        this.messageService = messageService;
        this.discoveryClient = discoveryClient;
    }

    @PostMapping
    public BroadcastVerificationResponse broadcast(@RequestBody BroadcastVerificationRequest request) {
        BroadcastResult result = messageService.broadcast(request.message());
        Map<String, List<MessageRecord>> state =
                messageService.fetchProviderState("broadcast-provider");
        return new BroadcastVerificationResponse(result, state);
    }

    @GetMapping("/instances")
    public List<String> instances() {
        return discoveryClient.getInstances("broadcast-provider").stream()
                .map(instance -> instance.getInstanceId() != null
                        ? instance.getInstanceId()
                        : instance.getHost() + ":" + instance.getPort())
                .toList();
    }
}
