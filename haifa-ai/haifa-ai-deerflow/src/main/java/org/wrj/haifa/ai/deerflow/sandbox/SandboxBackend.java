package org.wrj.haifa.ai.deerflow.sandbox;

public enum SandboxBackend {
    LOCAL_RESTRICTED("local-restricted"),
    LOCAL_TRUSTED("local-trusted"),
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
            return LOCAL_RESTRICTED;
        }
        if ("local".equalsIgnoreCase(value.trim())) {
            return LOCAL_RESTRICTED;
        }
        for (SandboxBackend backend : values()) {
            if (backend.id.equalsIgnoreCase(value.trim())) {
                return backend;
            }
        }
        throw new IllegalArgumentException("Unsupported sandbox backend: " + value
                + ". Supported values: local-restricted, local-trusted, docker");
    }
}
