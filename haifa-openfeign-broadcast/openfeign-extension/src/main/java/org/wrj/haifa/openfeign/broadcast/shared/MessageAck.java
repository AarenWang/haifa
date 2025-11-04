package org.wrj.haifa.openfeign.broadcast.shared;

import java.time.Instant;

/**
 * Response returned by provider instances when they accept a broadcast message.
 *
 * @param instanceId registry instance identifier
 * @param totalMessages number of messages stored by the instance
 * @param processedAt timestamp recorded by the instance
 */
public record MessageAck(String instanceId, int totalMessages, Instant processedAt) {}
