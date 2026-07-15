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
@MiddlewareLifecycle(MiddlewarePhase.RUN_PREPARATION)
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
        java.util.Map<String, Skill> activeByName = new java.util.LinkedHashMap<>();
        for (Skill skill : context.activeSkills()) {
            activeByName.put(skill.name(), skill);
        }
        for (Skill skill : slashSkillResolver.resolve(context.request().message())) {
            activeByName.putIfAbsent(skill.name(), skill);
        }
        List<Skill> activeSkills = new java.util.ArrayList<>(activeByName.values());
        List<Skill> availableSkills = skillStorage.listAll();
        AgentRuntimeContext enriched = context.withActiveSkills(activeSkills);
        return next.next(enriched).map(prompt -> {
            String skillsSection = SkillPromptRenderer.renderSkillSystem(
                    availableSkills,
                    activeSkills,
                    context.properties().getSkillsContainerPath()
            );
            String updatedSystem = SkillPromptRenderer.injectIntoSystemPrompt(prompt.systemPrompt(), skillsSection);
            return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
        });
    }
}
