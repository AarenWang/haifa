package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

/**
 * Assembles the final prompt before each model call.
 */
public class PromptAssembler {

    private final ConversationReplaySanitizer sanitizer;

    public PromptAssembler() {
        this(new ConversationReplaySanitizer());
    }

    public PromptAssembler(ConversationReplaySanitizer sanitizer) {
        this.sanitizer = sanitizer == null ? new ConversationReplaySanitizer() : sanitizer;
    }

    public Result assemble(String systemPrompt, String modelName, List<ModelMessage> messages) {
        List<ModelMessage> sanitizedMessages = this.sanitizer.sanitize(messages);
        String userPrompt = ModelPrompt.renderMessages(sanitizedMessages);
        ModelPrompt prompt = new ModelPrompt(systemPrompt, userPrompt, modelName, sanitizedMessages);
        PromptAssemblyTrace trace = buildTrace(prompt, sanitizedMessages);
        return new Result(prompt, trace, sanitizedMessages);
    }

    private PromptAssemblyTrace buildTrace(ModelPrompt prompt, List<ModelMessage> messages) {
        int historyTextChars = 0;
        int toolResultChars = 0;
        int summaryCount = 0;
        int dynamicReminderCount = 0;
        for (ModelMessage message : messages) {
            historyTextChars += message.content().length();
            if (message.role() == ModelMessage.Role.TOOL) {
                toolResultChars += message.content().length();
            }
            if (Boolean.TRUE.equals(message.metadata().get("isSummary"))) {
                summaryCount++;
            }
            if (Boolean.TRUE.equals(message.metadata().get("dynamicContextReminder"))) {
                dynamicReminderCount++;
            }
        }
        int estimatedTokens = TokenEstimator.estimateTokens(prompt.systemPrompt())
                + TokenEstimator.estimateTokens(messages);
        return new PromptAssemblyTrace(
                prompt.systemPrompt().length(),
                prompt.userPrompt().length(),
                messages.size(),
                historyTextChars,
                toolResultChars,
                estimatedTokens,
                summaryCount,
                0,
                dynamicReminderCount
        );
    }

    public record Result(ModelPrompt prompt, PromptAssemblyTrace trace, List<ModelMessage> messages) {}
}
