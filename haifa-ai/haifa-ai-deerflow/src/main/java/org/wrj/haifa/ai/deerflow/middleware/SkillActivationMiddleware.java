package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.skill.SkillPromptRenderer;
import org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver;
import org.wrj.haifa.ai.deerflow.skill.SkillStorage;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(5)
public class SkillActivationMiddleware implements AgentMiddleware {

    private final SlashSkillResolver slashSkillResolver;
    private final SkillStorage skillStorage;
    private final boolean enabled;

    public SkillActivationMiddleware(SlashSkillResolver slashSkillResolver, SkillStorage skillStorage, org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties) {
        this.slashSkillResolver = slashSkillResolver;
        this.skillStorage = skillStorage;
        this.enabled = properties.isSkillsEnabled();
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        if (!enabled) {
            return next.next(context);
        }
        List<Skill> activeSkills = new java.util.ArrayList<>(slashSkillResolver.resolve(context.request().message()));
        if (activeSkills.stream().noneMatch(s -> "local-script-execution".equals(s.name()))) {
            String message = context.request().message();
            if (shouldActivateLocalScriptExecution(message)) {
                skillStorage.findAny("local-script-execution").ifPresent(activeSkills::add);
            }
        }
        List<Skill> availableSkills = skillStorage.listAll();
        AgentRuntimeContext enriched = context.withActiveSkills(activeSkills);
        return next.next(enriched).map(prompt -> {
            String skillsSection = SkillPromptRenderer.renderSkillSystem(
                    availableSkills,
                    activeSkills,
                    context.properties().getSkillsRoot()
            );
            String updatedSystem = SkillPromptRenderer.injectIntoSystemPrompt(prompt.systemPrompt(), skillsSection);
            return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
        });
    }

    private boolean shouldActivateLocalScriptExecution(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String[] keywords = {
            "CPU", "内存", "memory", "cpu", "使用率", "当前电脑", "本机", "系统资源",
            "运行脚本", "执行脚本", "跑一下", "检查环境", "查看当前"
        };
        for (String kw : keywords) {
            if (message.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
