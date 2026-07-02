package org.wrj.haifa.ai.deerflow.agent.loop;

/**
 * Audit trace of how the final prompt was assembled.
 */
public record PromptAssemblyTrace(
    int systemPromptChars,
    int userPromptChars,
    int messageCount,
    int historyTextChars,
    int toolResultChars,
    int estimatedTokens,
    int summaryCount,
    int rescuedSkillCount,
    int dynamicReminderCount
) {}
