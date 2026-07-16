package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Files;
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

class RunScriptToolTest {

    @Test
    void runScriptDisabledReturnsSecurityMessage(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(false);
        properties.getSandbox().setEnabled(true);

        ToolResult result = new RunScriptTool(properties, new FakeSandboxRunner(), new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"print(1)\"}", tmp));

        assertThat(result.content()).contains("disabled by security configuration");
    }

    @Test
    void sandboxDisabledDoesNotExecute(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(false);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"print(1)\"}", tmp));

        assertThat(result.content()).contains("sandbox is disabled");
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void localBackendFailsByDefault(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setBackend("local");
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(false);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"print(1)\"}", tmp));

        assertThat(result.content()).contains("Local host execution is disabled");
        assertThat(result.metadata()).containsEntry("denied", true);
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void unknownBackendFailsClosed(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setBackend("dockr");
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(false);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"print(1)\"}", tmp));

        assertThat(result.content()).contains("Unsupported sandbox backend: dockr");
        assertThat(result.metadata()).containsEntry("denied", true);
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void languageNotAllowedReturnsError(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setAllowedScriptLanguages("python");
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"powershell\",\"code\":\"echo 1\"}", tmp));

        assertThat(result.content()).contains("not allowed by security configuration");
        assertThat(result.metadata()).containsEntry("denied", true);
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void emptyCodeReturnsError(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"\"}", tmp));

        assertThat(result.content()).contains("Error: code is required");
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void scriptBodyDeniedPatternFails(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        properties.getSandbox().setDeniedPatterns("os.remove,rm -rf");
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"import os\\nos.remove('test.txt')\"}", tmp));

        assertThat(result.content()).contains("Script execution denied: script code matches denied command rule: os.remove");
        assertThat(result.metadata()).containsEntry("denied", true);
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void scriptBodySensitivePathFails(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        FakeSandboxRunner runner = new FakeSandboxRunner();

        ToolResult result = new RunScriptTool(properties, runner, new CommandPolicy(properties))
                .execute(new ToolRequest("{\"language\":\"python\",\"code\":\"with open('/etc/passwd') as f: print(f.read())\"}", tmp));

        assertThat(result.content()).contains("Script execution denied: script code references a sensitive path: /etc/");
        assertThat(result.metadata()).containsEntry("denied", true);
        assertThat(runner.calls).isEqualTo(0);
    }

    @Test
    void executesScriptSuccessfullyAndMetadataPopulated(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setAllowedScriptLanguages("python,powershell");
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        properties.setWorkspaceRoot(tmp.resolve("workspace").toString());
        properties.setOutputsRoot(tmp.resolve("outputs").toString());

        FakeSandboxRunner runner = new FakeSandboxRunner();
        RunScriptTool tool = new RunScriptTool(properties, runner, new CommandPolicy(properties));

        ToolRequest request = new ToolRequest(
                "{\"language\":\"python\",\"code\":\"print('hello')\",\"args\":[\"arg1\"],\"purpose\":\"test\"}",
                tmp, java.util.List.of(), "thread-123", "run-456"
        );
        ToolResult result = tool.execute(request);

        assertThat(runner.calls).isEqualTo(1);
        assertThat(runner.lastRequest.runWorkingDirectory()).isNotNull();
        assertThat(Path.of(runner.lastRequest.cmdArgs().get(0))).isAbsolute();
        assertThat(runner.lastRequest.cmdArgs().subList(1, 3)).containsExactly("script.py", "arg1");

        Path scriptFolder = runner.lastRequest.runWorkingDirectory();
        assertThat(Files.exists(scriptFolder.resolve("script.py"))).isTrue();

        assertThat(result.content()).contains("Script language: python")
                .contains("Sandbox ID: sandbox-test-id")
                .contains("Stdout:\nhello-stdout");

        assertThat(result.metadata()).containsEntry("toolName", "run_script");
        assertThat(result.metadata()).containsEntry("language", "python");
        assertThat(result.metadata()).containsEntry("purpose", "test");
        assertThat(result.metadata()).containsEntry("sandboxId", "sandbox-test-id");
        assertThat(result.metadata()).containsEntry("exitCode", 0);
        assertThat(result.metadata()).containsEntry("stdout", "hello-stdout");
        assertThat(result.metadata()).containsEntry("scriptPath", "sandbox/run-456/scripts/" + scriptFolder.getFileName().toString() + "/script.py");
    }

    @Test
    void generatesCorrectCommandArgsForPowershell(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setAllowedScriptLanguages("powershell");
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        properties.setWorkspaceRoot(tmp.resolve("workspace").toString());
        properties.setOutputsRoot(tmp.resolve("outputs").toString());

        FakeSandboxRunner runner = new FakeSandboxRunner();
        RunScriptTool tool = new RunScriptTool(properties, runner, new CommandPolicy(properties));

        ToolRequest request = new ToolRequest(
                "{\"language\":\"powershell\",\"code\":\"echo 1\",\"args\":[],\"purpose\":\"test\"}",
                tmp, java.util.List.of(), "thread-123", "run-456"
        );
        tool.execute(request);

        assertThat(Path.of(runner.lastRequest.cmdArgs().get(0))).isAbsolute();
        assertThat(runner.lastRequest.cmdArgs().subList(1, 6)).containsExactly(
                "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "script.ps1");
    }

    @Test
    void generatesCorrectCommandArgsForBash(@TempDir Path tmp) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setRunScriptEnabled(true);
        properties.getSandbox().setEnabled(true);
        properties.getSandbox().setAllowedScriptLanguages("bash");
        properties.getSandbox().setRunScriptLocalUnsafeAllowed(true);
        properties.setWorkspaceRoot(tmp.resolve("workspace").toString());
        properties.setOutputsRoot(tmp.resolve("outputs").toString());

        FakeSandboxRunner runner = new FakeSandboxRunner();
        RunScriptTool tool = new RunScriptTool(properties, runner, new CommandPolicy(properties));

        ToolRequest request = new ToolRequest(
                "{\"language\":\"bash\",\"code\":\"echo 1\",\"args\":[],\"purpose\":\"test\"}",
                tmp, java.util.List.of(), "thread-123", "run-456"
        );
        tool.execute(request);

        assertThat(Path.of(runner.lastRequest.cmdArgs().get(0))).isAbsolute();
        assertThat(runner.lastRequest.cmdArgs().get(1)).isEqualTo("script.sh");
    }

    private static class FakeSandboxRunner implements SandboxRunner {
        private int calls;
        private SandboxRequest lastRequest;

        @Override
        public SandboxResult run(SandboxRequest request) {
            calls++;
            lastRequest = request;
            return new SandboxResult("sandbox-test-id", 0, "hello-stdout", "", 45, false, false,
                    Map.of("sandboxBackend", "local", "isolationLevel", "test"));
        }
    }
}
