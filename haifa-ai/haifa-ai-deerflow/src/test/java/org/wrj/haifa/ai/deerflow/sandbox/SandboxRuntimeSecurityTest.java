package org.wrj.haifa.ai.deerflow.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

class SandboxRuntimeSecurityTest {

    @Test
    void backendParsingIsExplicitAndFailsClosed() {
        assertThat(SandboxBackend.from(null)).isEqualTo(SandboxBackend.LOCAL_RESTRICTED);
        assertThat(SandboxBackend.from("local")).isEqualTo(SandboxBackend.LOCAL_RESTRICTED);
        assertThat(SandboxBackend.from("local-trusted")).isEqualTo(SandboxBackend.LOCAL_TRUSTED);
        assertThatThrownBy(() -> SandboxBackend.from("dockr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sandbox backend");
    }

    @Test
    void localTrustedRequiresBothExplicitGates() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getSandbox().setBackend("local-trusted");

        assertThat(SandboxExecutionPolicy.evaluate(properties, true).allowed()).isFalse();
        properties.getSandbox().setAllowHostExecution(true);
        assertThat(SandboxExecutionPolicy.evaluate(properties, true).allowed()).isFalse();
        properties.getSandbox().getLocalTrusted().setEnabled(true);
        assertThat(SandboxExecutionPolicy.evaluate(properties, true).allowed()).isTrue();
        assertThatThrownBy(() -> {
            DeerFlowProperties invalid = new DeerFlowProperties();
            invalid.getSandbox().setBackend("local-trusted");
            new SandboxConfigurationValidator(invalid).afterPropertiesSet();
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("requires allow-host-execution");
    }

    @Test
    void trustedEnvironmentFiltersSecretsAndHonorsControlledOverrides(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        TrustedEnvironmentPolicy policy = new TrustedEnvironmentPolicy(properties);
        assertThat(policy.shouldInherit("OPENAI_API_KEY")).isFalse();
        assertThat(policy.shouldInherit("MY_SERVICE_TOKEN")).isFalse();
        assertThat(policy.shouldInherit("PATH")).isTrue();

        properties.getSandbox().getLocalTrusted().setPassthroughEnvironmentNames(List.of("MY_SERVICE_TOKEN"));
        assertThat(policy.shouldInherit("MY_SERVICE_TOKEN")).isTrue();
        properties.getSandbox().getLocalTrusted().setPassthroughEnvironmentNames(List.of("JWT_SECRET"));
        assertThat(policy.shouldInherit("JWT_SECRET")).isFalse();

        HostShellResolver shellResolver = new HostShellResolver(properties);
        SandboxEnvironmentBuilder builder = new SandboxEnvironmentBuilder(properties, policy, shellResolver);
        Map<String, String> environment = builder.buildTrusted(
                Map.of("MINIMAX_API_KEY", "explicit-secret", "DEERFLOW_CJK_FONT", "font.ttf"),
                List.of(), tmp.resolve("work"));
        assertThat(environment).containsEntry("MINIMAX_API_KEY", "explicit-secret")
                .containsEntry("DEERFLOW_CJK_FONT", "font.ttf");

        SandboxSecretRedactor redactor = new SandboxSecretRedactor(policy);
        assertThat(redactor.redact("key=explicit-secret?token=visible", environment))
                .isEqualTo("key=[REDACTED]?token=[REDACTED]");
    }

    @Test
    void restrictedEnvironmentDoesNotAcceptPathOverride(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        SandboxEnvironmentBuilder builder = new SandboxEnvironmentBuilder(properties,
                new TrustedEnvironmentPolicy(properties), new HostShellResolver(properties));
        Map<String, String> environment = builder.buildRestricted(Map.of("PATH", "untrusted"), List.of(), tmp);
        assertThat(environment.get("PATH")).isEqualTo(System.getenv().getOrDefault("PATH", ""));
    }

    @Test
    void localTrustedReportsTruthfulMetadata(@TempDir Path tmp) {
        DeerFlowProperties properties = properties(tmp);
        LocalTrustedSandboxRunner runner = new LocalTrustedSandboxRunner(properties);
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                HostShellResolver.isWindows() ? "java.exe" : "java").toAbsolutePath();

        SandboxResult result = runner.run(new SandboxRequest(
                "java -version", List.of(javaExecutable.toString(), "-version"),
                tmp.resolve("workspace"), null, Duration.ofSeconds(5), Map.of(), false, "trusted-run"));

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.metadata()).containsEntry("sandboxBackend", "local-trusted")
                .containsEntry("isolationLevel", "trusted-host")
                .containsEntry("strongIsolation", false)
                .containsEntry("environmentPolicy", "inherit-filtered");
        assertThat(result.metadata().get("isolationWarning").toString()).contains("trusted single-user development");
    }

    @Test
    void utf8OutputDecoderPreservesChineseText() {
        ProcessOutputDecoder decoder = new ProcessOutputDecoder();
        assertThat(decoder.decode("中文图表生成成功".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("中文图表生成成功");
    }

    private static DeerFlowProperties properties(Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(tmp.resolve("workspace").toString());
        properties.setOutputsRoot(tmp.resolve("outputs").toString());
        properties.getSandbox().setWorkdirSubdir("sandbox");
        return properties;
    }
}
