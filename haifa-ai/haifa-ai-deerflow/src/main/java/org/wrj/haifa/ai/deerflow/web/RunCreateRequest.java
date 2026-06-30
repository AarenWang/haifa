package org.wrj.haifa.ai.deerflow.web;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;

public record RunCreateRequest(
        String threadId,
        @NotBlank String message,
        String model,
        List<String> uploadedFileIds,
        RunMode mode,
        ResearchOptions researchOptions
) {

    public RunCreateRequest(String threadId, String message, String model) {
        this(threadId, message, model, List.of(), RunMode.CHAT, ResearchOptions.defaults());
    }

    public RunCreateRequest(String threadId, String message, String model, List<String> uploadedFileIds) {
        this(threadId, message, model, uploadedFileIds == null ? List.of() : uploadedFileIds, RunMode.CHAT, ResearchOptions.defaults());
    }

    public RunCreateRequest(String threadId, String message, String model, List<String> uploadedFileIds,
            RunMode mode, ResearchOptions researchOptions) {
        this.threadId = threadId;
        this.message = message;
        this.model = model;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
        this.mode = mode == null ? RunMode.CHAT : mode;
        this.researchOptions = researchOptions == null ? ResearchOptions.defaults() : researchOptions;
    }

    public boolean isChatMode() {
        return this.mode == null || this.mode == RunMode.CHAT;
    }

    public boolean isResearchMode() {
        return this.mode == RunMode.RESEARCH;
    }
}
