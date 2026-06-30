package org.wrj.haifa.ai.deerflow.agent;

import java.util.List;

public record AgentRequest(String threadId, String message, String model, List<String> uploadedFileIds) {

    public AgentRequest(String threadId, String message, String model) {
        this(threadId, message, model, List.of());
    }

    public AgentRequest(String threadId, String message, String model, List<String> uploadedFileIds) {
        this.threadId = threadId;
        this.message = message;
        this.model = model;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
    }
}
