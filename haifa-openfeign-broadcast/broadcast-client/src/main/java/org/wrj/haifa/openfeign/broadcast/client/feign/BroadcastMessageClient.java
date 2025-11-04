package org.wrj.haifa.openfeign.broadcast.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.wrj.haifa.openfeign.broadcast.core.FeignBroadcast;
import org.wrj.haifa.openfeign.broadcast.model.BroadcastResult;
import org.wrj.haifa.openfeign.broadcast.shared.BroadcastMessageRequest;

@FeignClient(name = "broadcast-provider", path = "/api/messages")
public interface BroadcastMessageClient {

    @PostMapping
    @FeignBroadcast(failFast = false)
    BroadcastResult broadcast(@RequestBody BroadcastMessageRequest request);
}
