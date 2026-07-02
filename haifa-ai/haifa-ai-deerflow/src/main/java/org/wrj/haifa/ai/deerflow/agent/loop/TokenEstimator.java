package org.wrj.haifa.ai.deerflow.agent.loop;

import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import java.util.List;

/**
 * Approximate token estimator based on character length and language.
 */
public class TokenEstimator {

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chars = text.length();
        boolean hasChinese = false;
        for (int i = 0; i < chars; i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FA5) {
                hasChinese = true;
                break;
            }
        }
        if (hasChinese) {
            return (int) Math.ceil(chars / 2.0);
        } else {
            return (int) Math.ceil(chars / 4.0);
        }
    }

    public static int estimateTokens(ModelMessage message) {
        if (message == null) {
            return 0;
        }
        int tokens = 0;
        if (message.content() != null) {
            tokens += estimateTokens(message.content());
        }
        if (message.toolCalls() != null) {
            for (ModelToolCall tc : message.toolCalls()) {
                tokens += 10; // Base overhead per tool call
                if (tc.name() != null) tokens += estimateTokens(tc.name());
                if (tc.arguments() != null) tokens += estimateTokens(tc.arguments());
            }
        }
        if (message.toolCallId() != null) {
            tokens += estimateTokens(message.toolCallId());
        }
        return tokens;
    }

    public static int estimateTokens(List<ModelMessage> messages) {
        if (messages == null) {
            return 0;
        }
        int total = 0;
        for (ModelMessage m : messages) {
            total += estimateTokens(m);
        }
        return total;
    }
}
