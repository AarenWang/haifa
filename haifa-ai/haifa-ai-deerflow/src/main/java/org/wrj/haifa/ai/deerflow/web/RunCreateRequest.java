package org.wrj.haifa.ai.deerflow.web;

import jakarta.validation.constraints.NotBlank;

public record RunCreateRequest(String threadId, @NotBlank String message, String model) {
}
