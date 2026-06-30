package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.skill.SkillPromptRenderer;
import org.wrj.haifa.ai.deerflow.skill.SlashSkillResolver;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(5)
public class SkillActivationMiddleware implements AgentMiddleware {

    private final SlashSkillResolver slashSkillResolver;
    private final boolean enabled;

    public SkillActivationMiddleware(SlashSkillResolver slashSkillResolver, org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties) {
        this.slashSkillResolver = slashSkillResolver;
        this.enabled = properties.isSkillsEnabled();
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        if (!enabled) {
            return next.next(context);
        }
        List<Skill> activeSkills = slashSkillResolver.resolve(context.request().message());
        AgentRuntimeContext enriched = context.withActiveSkills(activeSkills);
        return next.next(enriched).map(prompt -> {
            String skillsSection = SkillPromptRenderer.renderActiveSkills(activeSkills);
            String updatedSystem = SkillPromptRenderer.injectIntoSystemPrompt(prompt.systemPrompt(), skillsSection);
            return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
        });
    }
}
