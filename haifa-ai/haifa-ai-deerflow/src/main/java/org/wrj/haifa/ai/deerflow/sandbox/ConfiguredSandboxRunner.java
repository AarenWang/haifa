package org.wrj.haifa.ai.deerflow.sandbox;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
@Primary
public class ConfiguredSandboxRunner implements SandboxRunner {

    private final DeerFlowProperties properties;
    private final LocalRestrictedSandboxRunner localRunner;
    private final LocalTrustedSandboxRunner trustedRunner;
    private final DockerSandboxRunner dockerRunner;

    public ConfiguredSandboxRunner(DeerFlowProperties properties, LocalRestrictedSandboxRunner localRunner,
            LocalTrustedSandboxRunner trustedRunner, DockerSandboxRunner dockerRunner) {
        this.properties = properties;
        this.localRunner = localRunner;
        this.trustedRunner = trustedRunner;
        this.dockerRunner = dockerRunner;
    }

    @Override
    public SandboxResult run(SandboxRequest request) {
        SandboxBackend backend = SandboxBackend.from(properties.getSandbox().getBackend());
        if (backend == SandboxBackend.LOCAL_TRUSTED
                && (!properties.getSandbox().isAllowHostExecution()
                || !properties.getSandbox().getLocalTrusted().isEnabled())) {
            throw new SecurityException("Local Trusted requires allow-host-execution=true and local-trusted.enabled=true");
        }
        return switch (backend) {
            case LOCAL_RESTRICTED -> localRunner.run(request);
            case LOCAL_TRUSTED -> trustedRunner.run(request);
            case DOCKER -> dockerRunner.run(request);
        };
    }
}
