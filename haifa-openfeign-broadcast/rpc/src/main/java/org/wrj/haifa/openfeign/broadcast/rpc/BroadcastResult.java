package org.wrj.haifa.openfeign.broadcast.rpc;

import java.util.List;

/**
 * Aggregated response emitted after a broadcast Feign invocation.
 *
 * @param serviceId target service id
 * @param failFast whether the broadcast short-circuits on failure
 * @param instances per-instance result details
 */
public record BroadcastResult(String serviceId, boolean failFast, List<InstanceResult> instances) {}
