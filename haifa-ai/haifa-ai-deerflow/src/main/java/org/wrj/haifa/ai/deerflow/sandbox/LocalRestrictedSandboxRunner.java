package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;

@Component
public class LocalRestrictedSandboxRunner extends AbstractLocalSandboxRunner {

    public LocalRestrictedSandboxRunner(DeerFlowProperties properties) {
        this(properties, new UserDataPathResolver(properties),
                new SandboxEnvironmentBuilder(properties, new TrustedEnvironmentPolicy(properties),
                        new HostShellResolver(properties)),
                new HostShellResolver(properties), new ProcessOutputDecoder(),
                new SandboxSecretRedactor(new TrustedEnvironmentPolicy(properties)));
    }

    @Autowired
    public LocalRestrictedSandboxRunner(DeerFlowProperties properties, UserDataPathResolver pathResolver,
            SandboxEnvironmentBuilder environmentBuilder, HostShellResolver shellResolver,
            ProcessOutputDecoder outputDecoder, SandboxSecretRedactor secretRedactor) {
        super(properties, pathResolver, environmentBuilder, shellResolver, outputDecoder, secretRedactor);
    }

    @Override
    protected Map<String, String> buildEnvironment(Map<String, String> explicitEnvironment,
            List<String> commandArgs, Path workdir) {
        return environmentBuilder().buildRestricted(explicitEnvironment, commandArgs, workdir);
    }

    @Override
    protected String backendId() {
        return SandboxBackend.LOCAL_RESTRICTED.id();
    }

    @Override
    protected String isolationLevel() {
        return "local-restricted";
    }

    @Override
    protected String environmentPolicy() {
        return "restricted-allowlist";
    }

    @Override
    protected String isolationWarning() {
        return "Local Restricted runs a host process with a controlled cwd and environment; use Docker for untrusted workloads.";
    }

    @Override
    protected boolean exposePhysicalWorkdir() {
        return true;
    }
}
