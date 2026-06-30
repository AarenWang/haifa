package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.agent.ResearchDepth;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.research.InMemoryEvidenceStore;
import org.wrj.haifa.ai.deerflow.research.InMemorySourceRegistry;

/**
 * Generates structured research plans from user queries using the configured model.
 * Falls back to heuristic-based planning when the model is unavailable or fails.
 */
@Component
public class ResearchPlanner {

    private final AgentModelClient modelClient;
    private final InMemorySourceRegistry sourceRegistry;
    private final InMemoryEvidenceStore evidenceStore;

    public ResearchPlanner(AgentModelClient modelClient, InMemorySourceRegistry sourceRegistry, InMemoryEvidenceStore evidenceStore) {
        this.modelClient = modelClient;
        this.sourceRegistry = sourceRegistry;
        this.evidenceStore = evidenceStore;
    }

    /**
     * Generate a research plan for the given topic and options.
     * First attempts model-driven planning; falls back to heuristic if the model fails.
     */
    public PlanGenerationResult generatePlan(String threadId, String runId, String topic, ResearchOptions options) {
        if (!StringUtils.hasText(topic)) {
            return PlanGenerationResult.failure("Topic is empty");
        }

        // Attempt model-driven plan generation
        try {
            PlanGenerationResult modelResult = generatePlanWithModel(threadId, runId, topic, options);
            if (modelResult.success() && modelResult.plan() != null) {
                return modelResult;
            }
        } catch (Exception e) {
            // Fall through to heuristic
        }

        // Heuristic fallback: generate dimensions from the topic itself
        return PlanGenerationResult.success(heuristicPlan(threadId, runId, topic, options));
    }

    private PlanGenerationResult generatePlanWithModel(String threadId, String runId, String topic, ResearchOptions options) {
        String prompt = buildPlanPrompt(topic, options);
        ModelPrompt modelPrompt = new ModelPrompt(
                "You are a research planning assistant. Generate structured research plans in JSON format.",
                prompt,
                null
        );
        ModelResponse response = modelClient.generate(modelPrompt).block();
        if (response == null || !StringUtils.hasText(response.content())) {
            return PlanGenerationResult.failure("Model returned empty response");
        }
        // For now, fallback to heuristic since we don't have a robust JSON parser for plan extraction
        // In a production system, this would parse the model response into a structured plan
        return PlanGenerationResult.failure("Model response parsing not yet implemented");
    }

    private String buildPlanPrompt(String topic, ResearchOptions options) {
        return """
                Given the research topic: "%s"
                Generate a research plan with the following structure:
                {
                  "researchQuestions": ["question1", "question2"],
                  "dimensions": [
                    {"title": "Dimension 1", "description": "...", "searchQueries": ["query1", "query2"]}
                  ],
                  "sourceCriteria": "...",
                  "expectedDeliverable": "..."
                }
                The plan should cover at least 3 different angles/dimensions.
                Depth: %s
                Max Sources: %d
                """.formatted(topic, options.depth().name(), options.maxSources());
    }

    /**
     * Heuristic plan generation based on topic keywords and research depth.
     */
    public ResearchPlan heuristicPlan(String threadId, String runId, String topic, ResearchOptions options) {
        String normalizedTopic = topic.toLowerCase();
        List<ResearchDimension> dimensions = new ArrayList<>();
        List<String> searchQueries = new ArrayList<>();
        int minTotalSources = switch (options.depth()) {
            case QUICK -> Math.min(options.maxSources(), 3);
            case DEEP -> Math.min(options.maxSources(), 6);
            case STANDARD -> Math.min(options.maxSources(), 5);
        };
        String timeHint = buildTimeHint(options);

        // Always add a general overview dimension
        dimensions.add(new ResearchDimension(
                UUID.randomUUID().toString(),
                "Overview & Context",
                "Broad understanding of the topic, its significance, and key stakeholders.",
                ResearchTaskStatus.PENDING,
                List.of(
                        topic + timeHint,
                        "introduction to " + topic + timeHint,
                        "what is " + topic + timeHint
                ),
                1, 0, 0, List.of()
        ));
        searchQueries.add(topic + timeHint);

        // Add dimensions based on topic keywords and depth
        int targetDimensions = options.depth() == ResearchDepth.QUICK ? 3 : (options.depth() == ResearchDepth.DEEP ? 5 : 4);

        if (normalizedTopic.contains("technology") || normalizedTopic.contains("tech") || normalizedTopic.contains("software")) {
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Technical Architecture & Implementation",
                    "How the technology works, key components, and implementation details.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " architecture", topic + " how it works", topic + " implementation" + timeHint),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Market Trends & Adoption",
                    "Current adoption rates, market size, and growth trends.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " market trends" + timeHint, topic + " adoption statistics", topic + " industry report"),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Challenges & Limitations",
                    "Known limitations, risks, and critical challenges.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " challenges", topic + " limitations", topic + " risks and concerns" + timeHint),
                    1, 0, 0, List.of()
            ));
        } else if (normalizedTopic.contains("company") || normalizedTopic.contains("stock") || normalizedTopic.contains("business")) {
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Financial Performance",
                    "Revenue, profit margins, growth metrics, and financial health.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " financial results" + timeHint, topic + " revenue earnings", topic + " stock performance"),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Competitive Landscape",
                    "Key competitors, market positioning, and competitive advantages.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " competitors", topic + " market share", topic + " competitive analysis" + timeHint),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Strategic Outlook & Risks",
                    "Future strategy, potential risks, and analyst opinions.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " strategy outlook", topic + " analyst rating", topic + " future risks" + timeHint),
                    1, 0, 0, List.of()
            ));
        } else {
            // Generic dimensions for any topic
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Key Facts & Data",
                    "Concrete statistics, numbers, and measurable data points.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " statistics" + timeHint, topic + " data numbers", topic + " quantitative analysis"),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Case Studies & Examples",
                    "Real-world applications, case studies, and practical examples.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " case study", topic + " example", topic + " real world application" + timeHint),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Expert Opinions & Perspectives",
                    "What experts, analysts, and stakeholders say about the topic.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " expert opinion", topic + " analyst commentary", topic + " expert review" + timeHint),
                    1, 0, 0, List.of()
            ));
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Challenges & Counter-Arguments",
                    "Criticisms, limitations, and opposing viewpoints for balanced coverage.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " criticism", topic + " challenges", topic + " counter arguments" + timeHint),
                    1, 0, 0, List.of()
            ));
        }

        // Ensure we have at least targetDimensions, but never more than 6
        while (dimensions.size() < targetDimensions && dimensions.size() < 6) {
            dimensions.add(new ResearchDimension(
                    UUID.randomUUID().toString(),
                    "Deep Dive: " + dimensions.size(),
                    "Additional detailed exploration of specific sub-topics.",
                    ResearchTaskStatus.PENDING,
                    List.of(topic + " detailed analysis" + timeHint),
                1, 0, 0, List.of()
            ));
        }
        while (dimensions.size() > targetDimensions) {
            dimensions.remove(dimensions.size() - 1);
        }

        distributeExpectedSources(dimensions, minTotalSources);
        for (ResearchDimension d : dimensions) {
            searchQueries.addAll(d.searchQueries());
        }

        return new ResearchPlan(
                UUID.randomUUID().toString(),
                threadId,
                runId,
                topic,
                List.of("What is the current state of " + topic + "?",
                        "What are the key trends and developments in " + topic + timeHint + "?",
                        "What are the main challenges and opportunities in " + topic + "?"),
                dimensions,
                searchQueries,
                "Prefer authoritative sources: academic papers, official documentation, reputable news outlets, and industry reports. Avoid unverified blogs and forum posts.",
                options.outputFormat().name().toLowerCase().contains("report")
                        ? "A comprehensive research report with executive summary, key findings, evidence-backed analysis, and a sources section."
                        : "A well-structured answer with inline citations and a sources section.",
                "CREATED",
                null, null
        );
    }

    private static void distributeExpectedSources(List<ResearchDimension> dimensions, int targetSources) {
        if (dimensions.isEmpty() || targetSources <= 0) {
            return;
        }
        int remaining = targetSources;
        List<ResearchDimension> updated = new ArrayList<>(dimensions.size());
        for (int i = 0; i < dimensions.size(); i++) {
            ResearchDimension dimension = dimensions.get(i);
            int dimensionsLeft = dimensions.size() - i;
            int assigned = Math.max(1, remaining / dimensionsLeft);
            remaining -= assigned;
            updated.add(new ResearchDimension(
                    dimension.id(),
                    dimension.title(),
                    dimension.description(),
                    dimension.status(),
                    dimension.searchQueries(),
                    assigned,
                    dimension.actualSourceCount(),
                    dimension.actualEvidenceCount(),
                    dimension.evidenceIds()
            ));
        }
        dimensions.clear();
        dimensions.addAll(updated);
    }

    private static String buildTimeHint(ResearchOptions options) {
        return switch (options.timeWindow()) {
            case LAST_30_DAYS -> " last 30 days";
            case LAST_YEAR -> " last year";
            case ALL_TIME -> " historical";
            case LATEST -> " latest";
        };
    }
}
