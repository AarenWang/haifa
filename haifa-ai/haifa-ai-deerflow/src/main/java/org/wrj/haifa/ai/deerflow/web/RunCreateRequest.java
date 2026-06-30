package org.wrj.haifa.ai.deerflow.web;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RunCreateRequest(String threadId, @NotBlank String message, String model, List<String> uploadedFileIds) {

    public RunCreateRequest(String threadId, String message, String model) {
        this(threadId, message, model, List.of());
    }

    public RunCreateRequest(String threadId, String message, String model, List<String> uploadedFileIds) {
        this.threadId = threadId;
        this.message = message;
        this.model = model;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
    }
}
