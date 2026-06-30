package org.wrj.haifa.ai.deerflow.middleware;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.skill.FileSystemSkillStorage;
import org.wrj.haifa.ai.deerflow.middleware.SkillActivationMiddleware;
import org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SkillActivationMiddlewareTest {

    @Test
    void injectsSkillContextIntoPrompt() throws Exception {
        Path publicDir = java.nio.file.Files.createTempDirectory("skills-public");
        java.nio.file.Files.createDirectories(publicDir.resolve("research"));
        java.nio.file.Files.writeString(publicDir.resolve("research").resolve("SKILL.md"), "# Research\nDo research.\n\n- allowed-tools: web_search");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("You are helpful.");
        properties.setWorkspaceRoot(".");
        properties.setCharBudget(0);

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, null);
        SlashSkillResolver resolver = new SlashSkillResolver(storage);
        SkillActivationMiddleware middleware = new SkillActivationMiddleware(resolver, storage, properties);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "/research summarize this", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(middleware));
        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.systemPrompt()).contains("You are helpful.");
                    assertThat(prompt.systemPrompt()).contains("<skill_system>");
                    assertThat(prompt.systemPrompt()).contains("research");
                    assertThat(prompt.systemPrompt()).contains("<location>");
                    assertThat(prompt.userPrompt()).contains("User request:");
                })
                .verifyComplete();
    }

    @Test
    void doesNothingWhenNoSlashSkill() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("base prompt");
        properties.setWorkspaceRoot(".");

        FileSystemSkillStorage storage = new FileSystemSkillStorage(null, null);
        SlashSkillResolver resolver = new SlashSkillResolver(storage);
        SkillActivationMiddleware middleware = new SkillActivationMiddleware(resolver, storage, properties);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "hello", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(middleware));
        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.systemPrompt()).contains("base prompt");
                    assertThat(prompt.userPrompt()).contains("User request:");
                })
                .verifyComplete();
    }

    @Test
    void skillActivationThenTokenBudgetStillWorks() throws Exception {
        Path publicDir = java.nio.file.Files.createTempDirectory("skills-public");
        java.nio.file.Files.createDirectories(publicDir.resolve("research"));
        java.nio.file.Files.writeString(publicDir.resolve("research").resolve("SKILL.md"), "# Research\nDo research.");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setSystemPrompt("base");
        properties.setWorkspaceRoot(".");
        properties.setCharBudget(50); // small budget that the final prompt will exceed

        FileSystemSkillStorage storage = new FileSystemSkillStorage(publicDir, null);
        SlashSkillResolver resolver = new SlashSkillResolver(storage);
        SkillActivationMiddleware skillMw = new SkillActivationMiddleware(resolver, storage, properties);

        AgentRunConfig config = new AgentRunConfig("t", "r", "m", false, false, 4, Path.of("."), java.util.Map.of());
        AgentRequest request = new AgentRequest("t", "/research do a very long thing with many words", null);
        AgentRuntimeContext context = AgentRuntimeContext.of(config, request, List.of(), properties);

        MiddlewareChain chain = new MiddlewareChain(List.of(skillMw, new TokenBudgetMiddleware()));
        StepVerifier.create(chain.next(context))
                .assertNext(prompt -> {
                    assertThat(prompt.systemPrompt()).contains("base");
                    assertThat(prompt.systemPrompt()).contains("<skill_system>");
                    assertThat(prompt.userPrompt()).startsWith("BUDGET_EXCEEDED:");
                })
                .verifyComplete();
    }
}
