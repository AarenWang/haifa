package org.wrj.haifa.openfeign.broadcast.client.api;

/**
 * REST payload to trigger a broadcast verification.
 *
 * @param message message content that will be broadcast to all provider instances
 */
public record BroadcastVerificationRequest(String message) {}
