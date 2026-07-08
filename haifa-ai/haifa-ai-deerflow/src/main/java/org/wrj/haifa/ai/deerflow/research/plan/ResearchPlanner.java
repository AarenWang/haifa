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
import org.wrj.haifa.ai.deerflow.research.EvidenceStore;
import org.wrj.haifa.ai.deerflow.research.SourceRegistry;

/**
 * Generates structured research plans from user queries using the configured model.
 * Falls back to heuristic-based planning when the model is unavailable or fails.
 */
@Component
public class ResearchPlanner {

    private final AgentModelClient modelClient;
    private final SourceRegistry sourceRegistry;
    private final EvidenceStore evidenceStore;

    public ResearchPlanner(AgentModelClient modelClient, SourceRegistry sourceRegistry, EvidenceStore evidenceStore) {
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
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "topic": {"type": "string"},
                    "objective": {"type": "string"},
                    "assumptions": {"type": "string"},
                    "dimensions": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "id": {"type": "string"},
                          "title": {"type": "string"},
                          "description": {"type": "string"},
                          "search_queries": {
                            "type": "array",
                            "items": {"type": "string"}
                          },
                          "expected_source_count": {"type": "integer"}
                        },
                        "required": ["id", "title"]
                      }
                    },
                    "quality_requirements": {"type": "string"},
                    "output_contract": {"type": "string"}
                  },
                  "required": ["topic", "objective", "dimensions"]
                }
                """;

        org.wrj.haifa.ai.deerflow.model.ModelToolDefinition createPlanTool = new org.wrj.haifa.ai.deerflow.model.ModelToolDefinition(
                "create_research_plan",
                "Call this tool to finalize the generated research plan with topic, objective, and dimensions.",
                schema
        );

        String prompt = buildPlanPrompt(topic, options);
        org.wrj.haifa.ai.deerflow.model.ModelPrompt modelPrompt = new org.wrj.haifa.ai.deerflow.model.ModelPrompt(
                "You are a research planning assistant. You MUST call the 'create_research_plan' tool to return the structured plan.",
                prompt,
                null,
                List.of(),
                List.of(createPlanTool)
        );

        org.wrj.haifa.ai.deerflow.model.ModelResponse response = modelClient.generate(modelPrompt).block();
        if (response == null || response.toolCalls().isEmpty()) {
            return PlanGenerationResult.failure("Model failed to call create_research_plan tool or returned empty response");
        }

        org.wrj.haifa.ai.deerflow.model.ModelToolCall call = response.toolCalls().get(0);
        if (!"create_research_plan".equals(call.name())) {
            return PlanGenerationResult.failure("Model called unexpected tool: " + call.name());
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(call.arguments());

            String planTopic = root.path("topic").asText(topic);
            String objective = root.path("objective").asText("");
            String assumptions = root.path("assumptions").asText("");
            String qualityRequirements = root.path("quality_requirements").asText("");
            String outputContract = root.path("output_contract").asText("");

            List<String> researchQuestions = new ArrayList<>();
            if (root.has("researchQuestions")) {
                for (com.fasterxml.jackson.databind.JsonNode q : root.path("researchQuestions")) {
                    researchQuestions.add(q.asText());
                }
            } else {
                researchQuestions.add(objective);
            }

            List<ResearchDimension> dimensions = new ArrayList<>();
            List<String> allSearchQueries = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode dimsNode = root.path("dimensions");
            if (dimsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode d : dimsNode) {
                    String dimId = d.path("id").asText(UUID.randomUUID().toString());
                    String title = d.path("title").asText("Dimension");
                    String desc = d.path("description").asText("");
                    int expectedCount = d.path("expected_source_count").asInt(3);

                    List<String> queries = new ArrayList<>();
                    com.fasterxml.jackson.databind.JsonNode qNode = d.path("search_queries");
                    if (qNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode q : qNode) {
                            queries.add(q.asText());
                            allSearchQueries.add(q.asText());
                        }
                    }
                    if (queries.isEmpty()) {
                        queries.add(planTopic + " " + title);
                        allSearchQueries.add(planTopic + " " + title);
                    }

                    dimensions.add(new ResearchDimension(
                            dimId, title, desc, ResearchTaskStatus.PENDING,
                            queries, expectedCount, 0, 0, List.of()
                    ));
                }
            }

            if (dimensions.isEmpty()) {
                return PlanGenerationResult.failure("No dimensions generated in research plan");
            }

            ResearchPlan plan = new ResearchPlan(
                    UUID.randomUUID().toString(),
                    threadId,
                    runId,
                    planTopic,
                    researchQuestions,
                    dimensions,
                    allSearchQueries,
                    qualityRequirements,
                    outputContract,
                    "CREATED",
                    java.time.Instant.now(),
                    java.time.Instant.now()
            );
            return PlanGenerationResult.success(plan);
        } catch (Exception e) {
            return PlanGenerationResult.failure("Failed to parse create_research_plan arguments: " + e.getMessage());
        }
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
