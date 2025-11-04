package org.wrj.haifa.openfeign.broadcast.client.api;

import java.util.List;
import java.util.Map;
import org.wrj.haifa.openfeign.broadcast.rpc.BroadcastResult;
import org.wrj.haifa.openfeign.broadcast.shared.MessageRecord;

/**
 * REST response containing broadcast results and per-instance state.
 *
 * @param result aggregated broadcast summary
 * @param instanceMessages messages stored on every provider instance
 */
public record BroadcastVerificationResponse(
        BroadcastResult result, Map<String, List<MessageRecord>> instanceMessages) {}
