package org.wrj.haifa.ai.spring.toolcalling.model;

/**
 * Request payload accepted by {@code GeoChatController}.
 */
public record ChatRequest(String message, String model) {
}
