package org.wrj.haifa.openfeign.broadcast.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.wrj.haifa.openfeign.broadcast.rpc.FeignBroadcast;

@FeignClient(name = "broadcast-provider", path = "/api/messages")
public interface BroadcastMessageClient {

    @PostMapping
    @FeignBroadcast(failFast = false)
    BroadcastResult broadcast(@RequestBody BroadcastMessageRequest request);
}
