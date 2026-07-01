package org.wrj.haifa.ai.deerflow.sandbox;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
@Primary
public class ConfiguredSandboxRunner implements SandboxRunner {

    private final DeerFlowProperties properties;
    private final LocalRestrictedSandboxRunner localRunner;
    private final DockerSandboxRunner dockerRunner;

    public ConfiguredSandboxRunner(DeerFlowProperties properties, LocalRestrictedSandboxRunner localRunner,
            DockerSandboxRunner dockerRunner) {
        this.properties = properties;
        this.localRunner = localRunner;
        this.dockerRunner = dockerRunner;
    }

    @Override
    public SandboxResult run(SandboxRequest request) {
        SandboxBackend backend = SandboxBackend.from(properties.getSandbox().getBackend());
        return backend == SandboxBackend.DOCKER ? dockerRunner.run(request) : localRunner.run(request);
    }
}
