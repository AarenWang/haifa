package org.wrj.haifa.ai.spring.toolcalling.model;

/**
 * Request payload accepted by the sample chat controllers.
 */
public record ChatRequest(String message, String model) {
}
