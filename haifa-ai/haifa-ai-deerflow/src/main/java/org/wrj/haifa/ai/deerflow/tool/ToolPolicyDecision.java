package org.wrj.haifa.ai.deerflow.tool;

public record ToolPolicyDecision(boolean allowed, String reason) {
    public static ToolPolicyDecision allow() {
        return new ToolPolicyDecision(true, "");
    }

    public static ToolPolicyDecision deny(String reason) {
        return new ToolPolicyDecision(false, reason == null || reason.isBlank() ? "not allowed by tool policy" : reason);
    }
}
