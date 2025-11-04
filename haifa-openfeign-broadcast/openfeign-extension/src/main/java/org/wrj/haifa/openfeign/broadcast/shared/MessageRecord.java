package org.wrj.haifa.openfeign.broadcast.shared;

import java.time.Instant;

/**
 * In-memory record stored by each provider instance for verification.
 *
 * @param message broadcast payload
 * @param origin client identifier
 * @param receivedAt timestamp recorded by the provider instance
 */
public record MessageRecord(String message, String origin, Instant receivedAt) {}
