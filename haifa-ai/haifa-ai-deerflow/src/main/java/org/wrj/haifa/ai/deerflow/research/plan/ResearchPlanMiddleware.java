package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareLifecycle;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewarePhase;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Mono;

/**
 * Middleware that injects the research plan into the system prompt for research mode runs.
 * Ensures the model is aware of the plan dimensions and research workflow.
 */
@Component
@MiddlewareOrder(50)
@MiddlewareLifecycle(MiddlewarePhase.MODEL_INPUT)
public class ResearchPlanMiddleware implements AgentMiddleware {

    private final ResearchPlanStore planStore;

    public ResearchPlanMiddleware(ResearchPlanStore planStore) {
        this.planStore = planStore;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        return next.next(context).map(prompt -> {
            if (context.config() == null || context.config().mode() != org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH) {
                return prompt;
            }

            ResearchPlan plan = planStore.findByRunId(context.config().runId()).orElse(null);
            if (plan == null) {
                return prompt;
            }

            String planSection = buildPlanPromptSection(plan);
            String newSystemPrompt = prompt.systemPrompt() + "\n\n" + planSection;
            return new ModelPrompt(newSystemPrompt, prompt.userPrompt(), prompt.modelName());
        });
    }

    private String buildPlanPromptSection(ResearchPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("<research_plan>\n");
        sb.append("Topic: ").append(plan.topic()).append("\n");
        if (!plan.researchQuestions().isEmpty()) {
            sb.append("Research Questions:\n");
            for (String q : plan.researchQuestions()) {
                sb.append("  - ").append(q).append("\n");
            }
        }
        sb.append("Dimensions to explore:\n");
        for (ResearchDimension dim : plan.dimensions()) {
            sb.append("  [").append(dim.status().name()).append("] ")
                    .append(dim.title()).append("\n");
            if (!dim.searchQueries().isEmpty()) {
                sb.append("    Suggested queries: ")
                        .append(String.join(", ", dim.searchQueries()))
                        .append("\n");
            }
        }
        sb.append("Source Criteria: ").append(plan.sourceCriteria()).append("\n");
        sb.append("Expected Deliverable: ").append(plan.expectedDeliverable()).append("\n");
        sb.append("</research_plan>\n");
        sb.append("\nIMPORTANT: You must explore at least ").append(plan.dimensionCount())
                .append(" dimensions before generating a final answer. ");
        sb.append("Work through each dimension systematically. ");
        sb.append("Do not stop after a single search. ");
        return sb.toString();
    }
}
