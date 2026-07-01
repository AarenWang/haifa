package org.wrj.haifa.ai.deerflow.sandbox;

public enum SandboxBackend {
    LOCAL("local"),
    DOCKER("docker");

    private final String id;

    SandboxBackend(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SandboxBackend from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        for (SandboxBackend backend : values()) {
            if (backend.id.equalsIgnoreCase(value.trim())) {
                return backend;
            }
        }
        return LOCAL;
    }
}
