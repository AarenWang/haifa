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
        assertThat(destructive.reason()).contains("denied pattern");

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
    void allowsAllCommandsWhenAllowListIsEmpty() {
        DeerFlowProperties props = new DeerFlowProperties();
        props.getSandbox().setAllowedCommands("");
        CommandPolicy policy = new CommandPolicy(props);

        assertThat(policy.evaluate("curl https://example.com", Path.of(".")).allowed()).isTrue();
        assertThat(policy.evaluate("uname -a", Path.of(".")).allowed()).isTrue();
    }

    @Test
    void rejectsShellControlOperatorsOutsideQuotes() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate("mvn -v && python -c \"print(1)\"", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("mvn -v & python -c \"print(1)\"", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("mvn -v | cat", Path.of(".")).allowed()).isFalse();
        assertThat(policy.evaluate("mvn -v > out.txt", Path.of(".")).allowed()).isFalse();
    }

    @Test
    void rejectsAbsoluteAndParentPathReferences() {
        CommandPolicy policy = new CommandPolicy(new DeerFlowProperties());

        assertThat(policy.evaluate("cat /etc/passwd", Path.of(".")).reason()).contains("sensitive path");
        assertThat(policy.evaluate("cat C:\\Users\\me\\secret.txt", Path.of(".")).reason()).contains("absolute host path");
        assertThat(policy.evaluate("cat ../secret.txt", Path.of(".")).reason()).contains("parent directory");
    }
}
