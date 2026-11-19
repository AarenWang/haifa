package org.wrj.haifa.ai.deerflow.prompt;

import java.util.List;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;

public final class TokenEstimator {
    private TokenEstimator() { }
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        boolean chinese = text.chars().anyMatch(c -> c >= 0x4E00 && c <= 0x9FA5);
        return (int) Math.ceil(text.length() / (chinese ? 2.0 : 4.0));
    }
    public static int estimateTokens(ModelMessage message) {
        if (message == null) return 0;
        int total = estimateTokens(message.content()) + estimateTokens(message.toolCallId());
        for (ModelToolCall call : message.toolCalls() == null ? List.<ModelToolCall>of() : message.toolCalls()) {
            total += 10 + estimateTokens(call.name()) + estimateTokens(call.arguments());
        }
        return total;
    }
    public static int estimateTokens(List<ModelMessage> messages) {
        return messages == null ? 0 : messages.stream().mapToInt(TokenEstimator::estimateTokens).sum();
    }
}
