package org.wrj.haifa.openfeign.broadcast.model;

/**
 * Captures the outcome for a single service instance during a broadcast call.
 *
 * @param instanceId registry instance identifier (or host:port fallback)
 * @param status HTTP status code, or -1 when transport failed
 * @param success whether the call completed successfully (2xx status)
 * @param body raw response payload or error message
 */
public record InstanceResult(String instanceId, int status, boolean success, String body) {}
