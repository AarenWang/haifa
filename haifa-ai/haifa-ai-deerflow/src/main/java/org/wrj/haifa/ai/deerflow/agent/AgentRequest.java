package org.wrj.haifa.ai.deerflow.agent;

import java.util.List;

public record AgentRequest(
        String threadId,
        String message,
        String model,
        List<String> uploadedFileIds,
        RunMode mode,
        ResearchOptions researchOptions,
        String userId,
        java.util.Map<String, Object> metadata
) {

    public AgentRequest(String threadId, String message, String model) {
        this(threadId, message, model, List.of(), RunMode.CHAT, ResearchOptions.defaults(), "default-user", java.util.Map.of());
    }

    public AgentRequest(String threadId, String message, String model, List<String> uploadedFileIds) {
        this(threadId, message, model, uploadedFileIds == null ? List.of() : uploadedFileIds, RunMode.CHAT,
                ResearchOptions.defaults(), "default-user", java.util.Map.of());
    }

    public AgentRequest(String threadId, String message, String model, List<String> uploadedFileIds,
            RunMode mode, ResearchOptions researchOptions) {
        this(threadId, message, model, uploadedFileIds, mode, researchOptions, "default-user", java.util.Map.of());
    }

    public AgentRequest(String threadId, String message, String model, List<String> uploadedFileIds,
            RunMode mode, ResearchOptions researchOptions, String userId) {
        this(threadId, message, model, uploadedFileIds, mode, researchOptions, userId, java.util.Map.of());
    }

    public AgentRequest(String threadId, String message, String model, List<String> uploadedFileIds,
            RunMode mode, ResearchOptions researchOptions, String userId, java.util.Map<String, Object> metadata) {
        this.threadId = threadId;
        this.message = message;
        this.model = model;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
        this.mode = mode == null ? RunMode.CHAT : mode;
        this.researchOptions = researchOptions == null ? ResearchOptions.defaults() : researchOptions;
        this.userId = userId == null ? "default-user" : userId;
        this.metadata = metadata == null ? java.util.Map.of() : java.util.Map.copyOf(metadata);
    }


    public boolean isChatMode() {
        return this.mode == null || this.mode == RunMode.CHAT;
    }

    public boolean isResearchMode() {
        return this.mode == RunMode.RESEARCH;
    }
}
