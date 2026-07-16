package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import static org.assertj.core.api.Assertions.assertThat;

class CommandPolicyTest {

    @Test
    void allowsConfiguredCommands() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate("pwd", Path.of(".")).allowed()).isTrue();
        assertThat(policy.evaluate("mvn -pl haifa-ai/haifa-ai-deerflow test", Path.of(".")).allowed()).isTrue();
    }

    @Test
    void rejectsDeniedPatternsAndSensitivePaths() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        CommandPolicy.Decision destructive = policy.evaluate("rm -rf /", Path.of("."));
        assertThat(destructive.allowed()).isFalse();
        assertThat(destructive.reason()).contains("denied command rule");

        CommandPolicy.Decision gitAccess = policy.evaluate("cat .git/config", Path.of("."));
        assertThat(gitAccess.allowed()).isFalse();
        assertThat(gitAccess.reason()).contains("sensitive path");
    }

    @Test
    void rejectsCommandsOutsideAllowList() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        CommandPolicy.Decision decision = policy.evaluate("curl https://example.com", Path.of("."));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("allowed command list");
    }

    @Test
    void rejectsAllCommandsWhenAllowListIsEmpty() {
        DeerFlowProperties props = new DeerFlowProperties();
        props.getSandbox().setAllowedCommands("");
        CommandPolicy policy = new CommandPolicy(props);

        assertThat(policy.evaluate("curl https://example.com", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("uname -a", Path.of(".")).reason()).contains("allowed command list is empty");
    }

    @Test
    void rejectsShellControlOperatorsOutsideQuotes() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate("mvn -v && python -c \"print(1)\"", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("mvn -v & python -c \"print(1)\"", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("mvn -v | cat", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("python -c \"print('a|b')\"", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("mvn -v > out.txt", Path.of(".")).allowed()).isFalse();
    }

    @Test
    void rejectsPipeCharactersAndDeniedCommandsInScripts() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluateScriptBody("Get-Process | Select-Object Name").reason())
                .contains("disabled pipe character");
        // Safe pipes (inside quotes, comments, or part of double pipe ||) are allowed
        assertThat(policy.evaluateScriptBody("$pattern = '^(System|Idle)$'").allowed()).isTrue();
        assertThat(policy.evaluateScriptBody("if (a || b) {}").allowed()).isTrue();
        assertThat(policy.evaluateScriptBody("a = 1 | 2").reason())
                .contains("disabled pipe character"); // single pipe outside quotes is denied
        assertThat(policy.evaluateScriptBody("# comment with | pipe").allowed()).isTrue();
        assertThat(policy.evaluateScriptBody("/* block | comment */ a || b").allowed()).isTrue();

        assertThat(policy.evaluateScriptBody("rm -rf workspace").reason())
                .contains("denied command rule");
        assertThat(policy.evaluateScriptBody("Remove-Item data -Recurse").reason())
                .contains("denied command rule");
    }

    @Test
    void deniedRulesUseCommandTokenBoundariesInsteadOfSubstrings() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluateScriptBody("$name = 'secureboot'; Write-Output $name").allowed()).isTrue();
        assertThat(policy.evaluateScriptBody("Export-Csv result.csv -NoTypeInformation").allowed()).isTrue();
        assertThat(policy.evaluateScriptBody("Format-Table -AutoSize").allowed()).isTrue();
        assertThat(policy.evaluateScriptBody("reboot").allowed()).isFalse();
        assertThat(policy.evaluateScriptBody("format-volume -DriveLetter D").allowed()).isFalse();
    }

    @Test
    void allowListRequiresBareNormalizedExecutableNames() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate("mvn.exe -v", Path.of(".")).allowed()).isTrue();
        assertThat(policy.evaluate("mvn.cmd -v", Path.of(".")).allowed()).isTrue();
        assertThat(policy.evaluate(".\\mvn.exe -v", Path.of(".")).reason())
                .contains("bare command name");
    }

    @Test
    void rejectsAbsoluteAndParentPathReferences() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate("cat /etc/passwd", Path.of(".")).reason()).contains("sensitive path");
        assertThat(policy.evaluate("cat C:\\Users\\me\\secret.txt", Path.of(".")).reason()).contains("absolute host path");
        assertThat(policy.evaluate("cat ../secret.txt", Path.of(".")).reason()).contains("parent directory");
    }

    @Test
    void allowsOnlyRecognizedVirtualAbsoluteRoots() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate(
                "python /mnt/skills/public/image-generation/scripts/generate.py --output-file=/mnt/user-data/outputs/result.png",
                Path.of(".")).allowed()).isTrue();
        assertThat(policy.evaluate("cat /mnt/user-data/uploads/input.txt", Path.of(".")).allowed()).isTrue();
        assertThat(policy.evaluate("cat /tmp/secret.txt", Path.of(".")).reason())
                .contains("absolute host path");
    }
}
