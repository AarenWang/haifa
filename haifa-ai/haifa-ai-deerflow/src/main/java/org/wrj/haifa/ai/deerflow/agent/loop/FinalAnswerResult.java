package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.Map;

/**
 * Immutable record representing the final answer produced by the agent loop.
 * Carries the answer text along with any extra metadata added by observers.
 */
public record FinalAnswerResult(String finalAnswer, Map<String, Object> extraMetadata) {
}
