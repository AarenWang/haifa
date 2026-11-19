package org.wrj.haifa.ai.deerflow.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InMemoryEvidenceStore implements EvidenceStore {

    private final Map<String, EvidenceItem> evidenceById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> evidenceIdsByThread = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> evidenceIdsByRun = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> evidenceIdsBySource = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> evidenceIdsByDimension = new ConcurrentHashMap<>();
    private final Map<String, String> evidenceIdBySignature = new ConcurrentHashMap<>();

    @Override
    public EvidenceItem save(EvidenceItem evidenceItem) {
        String signature = signatureOf(evidenceItem.sourceId(), evidenceItem.claim(), evidenceItem.quoteOrParaphrase());
        String existingId = this.evidenceIdBySignature.get(signature);
        if (existingId != null) {
            EvidenceItem existing = this.evidenceById.get(existingId);
            if (existing != null) {
                index(existing);
                return existing;
            }
        }
        String evidenceId = StringUtils.hasText(evidenceItem.evidenceId())
                ? evidenceItem.evidenceId() : UUID.randomUUID().toString();
        EvidenceItem stored = new EvidenceItem(
                evidenceId,
                evidenceItem.threadId(),
                evidenceItem.runId(),
                evidenceItem.sourceId(),
                evidenceItem.quoteOrParaphrase(),
                evidenceItem.claim(),
                evidenceItem.dimension(),
                evidenceItem.confidence(),
                evidenceItem.extractedAt()
        );
        this.evidenceById.put(stored.evidenceId(), stored);
        this.evidenceIdBySignature.put(signature, stored.evidenceId());
        index(stored);
        return stored;
    }

    @Override
    public List<EvidenceItem> listByThread(String threadId) {
        return list(this.evidenceIdsByThread.getOrDefault(threadId, Set.of()));
    }

    @Override
    public List<EvidenceItem> listByRun(String runId) {
        return list(this.evidenceIdsByRun.getOrDefault(runId, Set.of()));
    }

    @Override
    public List<EvidenceItem> listBySourceId(String sourceId) {
        return list(this.evidenceIdsBySource.getOrDefault(sourceId, Set.of()));
    }

    @Override
    public List<EvidenceItem> listByDimension(String dimension) {
        return list(this.evidenceIdsByDimension.getOrDefault(normalize(dimension), Set.of()));
    }

    @Override
    public Optional<EvidenceItem> findById(String evidenceId) {
        return Optional.ofNullable(this.evidenceById.get(evidenceId));
    }

    private void index(EvidenceItem stored) {
        this.evidenceIdsByThread.computeIfAbsent(stored.threadId(), ignored -> ConcurrentHashMap.newKeySet()).add(stored.evidenceId());
        this.evidenceIdsByRun.computeIfAbsent(stored.runId(), ignored -> ConcurrentHashMap.newKeySet()).add(stored.evidenceId());
        this.evidenceIdsBySource.computeIfAbsent(stored.sourceId(), ignored -> ConcurrentHashMap.newKeySet()).add(stored.evidenceId());
        this.evidenceIdsByDimension.computeIfAbsent(normalize(stored.dimension()), ignored -> ConcurrentHashMap.newKeySet())
                .add(stored.evidenceId());
    }

    private List<EvidenceItem> list(Set<String> evidenceIds) {
        List<EvidenceItem> evidenceItems = new ArrayList<>();
        for (String evidenceId : evidenceIds) {
            EvidenceItem evidenceItem = this.evidenceById.get(evidenceId);
            if (evidenceItem != null) {
                evidenceItems.add(evidenceItem);
            }
        }
        evidenceItems.sort(Comparator.comparing(EvidenceItem::extractedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return evidenceItems;
    }

    private static String signatureOf(String sourceId, String claim, String quote) {
        return normalize(sourceId) + "|" + normalize(claim) + "|" + normalize(quote);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
