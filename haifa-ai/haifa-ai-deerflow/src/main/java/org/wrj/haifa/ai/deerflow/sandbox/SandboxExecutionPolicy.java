package org.wrj.haifa.ai.deerflow.sandbox;

import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

public final class SandboxExecutionPolicy {

    private SandboxExecutionPolicy() {
    }

    public static Decision evaluate(DeerFlowProperties properties, boolean runScript) {
        SandboxBackend backend;
        try {
            backend = SandboxBackend.from(properties.getSandbox().getBackend());
        } catch (IllegalArgumentException ex) {
            return Decision.deny(ex.getMessage());
        }
        if (backend == SandboxBackend.DOCKER) {
            return Decision.allow(backend);
        }
        if (backend == SandboxBackend.LOCAL_TRUSTED) {
            if (!properties.getSandbox().isAllowHostExecution()) {
                return Decision.deny("Local Trusted requires haifa.ai.deerflow.sandbox.allow-host-execution=true");
            }
            if (!properties.getSandbox().getLocalTrusted().isEnabled()) {
                return Decision.deny("Local Trusted requires haifa.ai.deerflow.sandbox.local-trusted.enabled=true");
            }
            return Decision.allow(backend);
        }
        boolean legacyRunScriptGrant = runScript && properties.getSandbox().isRunScriptLocalUnsafeAllowed();
        if (!properties.getSandbox().isAllowHostExecution() && !legacyRunScriptGrant) {
            return Decision.deny("Local host execution is disabled. Set haifa.ai.deerflow.sandbox.allow-host-execution=true or use docker.");
        }
        return Decision.allow(backend);
    }

    public record Decision(boolean allowed, SandboxBackend backend, String reason) {
        static Decision allow(SandboxBackend backend) {
            return new Decision(true, backend, "");
        }

        static Decision deny(String reason) {
            return new Decision(false, null, reason == null ? "denied" : reason);
        }
    }
}
