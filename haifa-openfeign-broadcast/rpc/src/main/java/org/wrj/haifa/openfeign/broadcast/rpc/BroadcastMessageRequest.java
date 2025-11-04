package org.wrj.haifa.openfeign.broadcast.rpc;

/**
 * Payload used when broadcasting a message to all service instances.
 *
 * @param origin identifier of the client that triggered the broadcast
 * @param message arbitrary message payload
 */
public record BroadcastMessageRequest(String origin, String message) {}
