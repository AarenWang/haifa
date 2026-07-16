package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;

@Component
public class LocalTrustedSandboxRunner extends AbstractLocalSandboxRunner {

    public LocalTrustedSandboxRunner(DeerFlowProperties properties) {
        this(properties, new UserDataPathResolver(properties),
                new SandboxEnvironmentBuilder(properties, new TrustedEnvironmentPolicy(properties),
                        new HostShellResolver(properties)),
                new HostShellResolver(properties), new ProcessOutputDecoder(),
                new SandboxSecretRedactor(new TrustedEnvironmentPolicy(properties)));
    }

    @Autowired
    public LocalTrustedSandboxRunner(DeerFlowProperties properties, UserDataPathResolver pathResolver,
            SandboxEnvironmentBuilder environmentBuilder, HostShellResolver shellResolver,
            ProcessOutputDecoder outputDecoder, SandboxSecretRedactor secretRedactor) {
        super(properties, pathResolver, environmentBuilder, shellResolver, outputDecoder, secretRedactor);
    }

    @Override
    protected Map<String, String> buildEnvironment(Map<String, String> explicitEnvironment,
            List<String> commandArgs, Path workdir) {
        return environmentBuilder().buildTrusted(explicitEnvironment, commandArgs, workdir);
    }

    @Override
    protected String backendId() {
        return SandboxBackend.LOCAL_TRUSTED.id();
    }

    @Override
    protected String isolationLevel() {
        return "trusted-host";
    }

    @Override
    protected String environmentPolicy() {
        return "inherit-filtered";
    }

    @Override
    protected String isolationWarning() {
        return "Local Trusted executes host processes and is intended only for trusted single-user development. Use Docker for untrusted workloads.";
    }
}
