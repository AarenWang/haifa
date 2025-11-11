package org.wrj.haifa.ai.spring.toolcalling.model;

/**
 * Simplified response that surfaces the generated answer back to API clients.
 */
public record ChatResponsePayload(String answer) {
}
