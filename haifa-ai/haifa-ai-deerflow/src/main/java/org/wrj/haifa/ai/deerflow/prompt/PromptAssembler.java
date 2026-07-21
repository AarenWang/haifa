package org.wrj.haifa.ai.deerflow.prompt;

import java.util.List;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

public class PromptAssembler {
    private final ConversationReplaySanitizer sanitizer;
    public PromptAssembler() { this(new ConversationReplaySanitizer()); }
    public PromptAssembler(ConversationReplaySanitizer sanitizer) {
        this.sanitizer = sanitizer == null ? new ConversationReplaySanitizer() : sanitizer;
    }
    public Result assemble(String systemPrompt, String modelName, List<ModelMessage> messages) {
        List<ModelMessage> sanitized = sanitizer.sanitize(messages);
        ModelPrompt prompt = new ModelPrompt(systemPrompt, ModelPrompt.renderMessages(sanitized), modelName, sanitized);
        int historyChars = sanitized.stream().mapToInt(m -> m.content().length()).sum();
        int toolChars = sanitized.stream().filter(m -> m.role() == ModelMessage.Role.TOOL)
                .mapToInt(m -> m.content().length()).sum();
        int summaries = (int) sanitized.stream().filter(m -> Boolean.TRUE.equals(m.metadata().get("isSummary"))).count();
        int reminders = (int) sanitized.stream().filter(m -> Boolean.TRUE.equals(m.metadata().get("dynamicContextReminder"))).count();
        PromptAssemblyTrace trace = new PromptAssemblyTrace(prompt.systemPrompt().length(), prompt.userPrompt().length(),
                sanitized.size(), historyChars, toolChars,
                TokenEstimator.estimateTokens(prompt.systemPrompt()) + TokenEstimator.estimateTokens(sanitized),
                summaries, 0, reminders);
        return new Result(prompt, trace, sanitized);
    }
    public record Result(ModelPrompt prompt, PromptAssemblyTrace trace, List<ModelMessage> messages) { }
}
