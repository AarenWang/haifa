package org.wrj.haifa.ai.deerflow.middleware;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.memory.MemoryFactRecord;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.persistence.store.MemoryFactStore;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@MiddlewareOrder(15)
@MiddlewareLifecycle(MiddlewarePhase.RUN_PREPARATION)
public class StructuredMemoryMiddleware implements AgentMiddleware {

    private final MemoryFactStore factStore;
    private static final int MAX_CHAR_BUDGET = 2000;

    public StructuredMemoryMiddleware(MemoryFactStore factStore) {
        this.factStore = factStore;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        String userId = "default-user";
        if (context.config() != null && context.config().metadata() != null) {
            Object metaUserId = context.config().metadata().get("userId");
            if (metaUserId instanceof String s && !s.isBlank()) {
                userId = s;
            }
        }

        List<MemoryFactRecord> activeFacts = factStore.findByUserIdAndStatus(userId, "active");
        if (activeFacts.isEmpty()) {
            return next.next(context);
        }

        String userMessage = context.request().message();
        List<MemoryFactRecord> rankedFacts = rankFacts(activeFacts, userMessage);

        StringBuilder memoryBuilder = new StringBuilder();
        memoryBuilder.append("\n<system-reminder>\n<memory>\nRelevant long-term user memory:\n");

        List<MemoryFactRecord> selectedFacts = new ArrayList<>();
        int currentLength = 0;

        for (MemoryFactRecord fact : rankedFacts) {
            String entry = "- " + fact.content() + "\n";
            if (currentLength + entry.length() > MAX_CHAR_BUDGET) {
                break;
            }
            memoryBuilder.append(entry);
            currentLength += entry.length();
            selectedFacts.add(fact);
        }

        memoryBuilder.append("</memory>\n</system-reminder>\n");

        if (selectedFacts.isEmpty()) {
            return next.next(context);
        }

        // Asynchronously update lastUsedAt for selected facts
        Mono.fromRunnable(() -> {
            for (MemoryFactRecord fact : selectedFacts) {
                MemoryFactRecord updated = new MemoryFactRecord(
                        fact.id(),
                        fact.userId(),
                        fact.agentId(),
                        fact.category(),
                        fact.content(),
                        fact.source(),
                        fact.sourceThreadId(),
                        fact.sourceRunId(),
                        fact.confidence(),
                        fact.status(),
                        fact.sourceError(),
                        fact.createdAt(),
                        Instant.now(), // updatedAt
                        Instant.now()  // lastUsedAt
                );
                factStore.save(updated);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();

        return next.next(context).map(prompt -> {
            String baseSystem = prompt.systemPrompt();
            String updatedSystem = (baseSystem == null || baseSystem.isBlank())
                    ? memoryBuilder.toString().trim()
                    : baseSystem + "\n" + memoryBuilder.toString().trim();
            return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
        });
    }

    private List<MemoryFactRecord> rankFacts(List<MemoryFactRecord> facts, String userMessage) {
        Set<String> keywords = extractKeywords(userMessage);

        class FactWithScore {
            final MemoryFactRecord fact;
            final double score;

            FactWithScore(MemoryFactRecord fact, double score) {
                this.fact = fact;
                this.score = score;
            }
        }

        List<FactWithScore> scored = new ArrayList<>();
        for (MemoryFactRecord fact : facts) {
            double score = getCategoryBaseScore(fact.category());

            // Keyword matching boost
            if (!keywords.isEmpty() && fact.content() != null) {
                String lowercaseContent = fact.content().toLowerCase();
                int matches = 0;
                for (String kw : keywords) {
                    if (lowercaseContent.contains(kw)) {
                        matches++;
                    }
                }
                score += matches * 20.0;
            }

            scored.add(new FactWithScore(fact, score));
        }

        // Sort by score desc, then lastUsedAt desc, then updatedAt desc
        return scored.stream()
                .sorted((f1, f2) -> {
                    if (Double.compare(f2.score, f1.score) != 0) {
                        return Double.compare(f2.score, f1.score);
                    }
                    Instant u1 = f1.fact.lastUsedAt() != null ? f1.fact.lastUsedAt() : f1.fact.updatedAt();
                    Instant u2 = f2.fact.lastUsedAt() != null ? f2.fact.lastUsedAt() : f2.fact.updatedAt();
                    if (u1 == null && u2 == null) return 0;
                    if (u1 == null) return 1;
                    if (u2 == null) return -1;
                    return u2.compareTo(u1);
                })
                .map(f -> f.fact)
                .toList();
    }

    private double getCategoryBaseScore(String category) {
        if (category == null) return 30.0;
        return switch (category.toLowerCase()) {
            case "constraint" -> 100.0;
            case "correction" -> 90.0;
            case "preference" -> 80.0;
            case "identity" -> 70.0;
            case "project_context" -> 60.0;
            case "goal" -> 50.0;
            case "reinforcement" -> 40.0;
            default -> 30.0;
        };
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null || text.isBlank()) {
            return keywords;
        }
        String[] words = text.toLowerCase().split("\\W+");
        for (String w : words) {
            if (w.length() >= 3 && !isStopWord(w)) {
                keywords.add(w);
            }
        }
        return keywords;
    }

    private boolean isStopWord(String word) {
        return word.equals("the") || word.equals("and") || word.equals("you") || word.equals("that")
                || word.equals("for") || word.equals("this") || word.equals("with") || word.equals("have");
    }
}
