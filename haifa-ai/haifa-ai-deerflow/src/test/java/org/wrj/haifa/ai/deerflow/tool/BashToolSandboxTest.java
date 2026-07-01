package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxRequest;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxResult;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BashToolSandboxTest {

    @Test
    void bashDisabledReturnsSecurityMessage(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setBashEnabled(false);
        properties.getSandbox().setEnabled(true);

        ToolResult result = new BashTool(properties, new FakeSandboxRunner(), new CommandPolicy(properties))
                .execute(new ToolRequest("{\"command\":\"pwd\"}", tmp));

        assertThat(result.content()).contains("disabled by security configuration");
    }

    @Test
    void sandboxDisabledDoesNotExecuteCommand(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setBashEnabled(true);
        properties.getSandbox().setEnabled(false);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new BashTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"command\":\"pwd\"}", tmp));

        assertThat(result.content()).contains("sandbox is disabled");
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void wrapsSandboxResultIntoStructuredMetadata(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setBashEnabled(true);
        properties.setOutputsRoot(tmp.resolve("outputs").toString());
        properties.getSandbox().setEnabled(true);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new BashTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"command\":\"pwd\"}", tmp, java.util.List.of(), "thread-1", "run-1"));

        assertThat(runner.calls).isEqualTo(1);
        assertThat(runner.lastRequest.runWorkingDirectory()).isNull();
        assertThat(result.content()).contains("Sandbox ID: sandbox-test").contains("Exit code: 0");
        assertThat(result.metadata()).containsEntry("sandboxId", "sandbox-test");
        assertThat(result.metadata()).containsEntry("exitCode", 0);
        assertThat(result.metadata()).containsEntry("stdout", "hello\n");
        assertThat(result.metadata()).containsEntry("timedOut", false);
    }

    @Test
    void deniedCommandReturnsDeniedMetadata(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setBashEnabled(true);
        properties.getSandbox().setEnabled(true);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new BashTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"command\":\"rm -rf /\"}", tmp));

        assertThat(result.content()).contains("Command denied");
        assertThat(result.metadata()).containsEntry("denied", true);
        assertThat(runner.calls).isEqualTo(0);
    }

    private static class FakeSandboxRunner implements SandboxRunner {
        private int calls;
        private SandboxRequest lastRequest;

        @Override
        public SandboxResult run(SandboxRequest request) {
            calls++;
            lastRequest = request;
            return new SandboxResult("sandbox-test", 0, "hello\n", "", 12, false, false,
                    Map.of("sandboxBackend", "local", "isolationLevel", "test"));
        }
    }
}
