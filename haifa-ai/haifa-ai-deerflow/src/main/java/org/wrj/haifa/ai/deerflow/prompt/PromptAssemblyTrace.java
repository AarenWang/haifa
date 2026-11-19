package org.wrj.haifa.ai.deerflow.prompt;

public record PromptAssemblyTrace(int systemPromptChars, int userPromptChars, int messageCount,
        int historyTextChars, int toolResultChars, int estimatedTokens, int summaryCount,
        int rescuedSkillCount, int dynamicReminderCount) { }
