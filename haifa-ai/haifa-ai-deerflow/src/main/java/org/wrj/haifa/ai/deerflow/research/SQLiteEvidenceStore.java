package org.wrj.haifa.ai.deerflow.research;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.persistence.entity.EvidenceItemEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.EvidenceItemRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Primary
public class SQLiteEvidenceStore implements EvidenceStore {

    private final EvidenceItemRepository repository;

    public SQLiteEvidenceStore(EvidenceItemRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EvidenceItem save(EvidenceItem evidenceItem) {
        if (evidenceItem == null) {
            return null;
        }
        String signature = signatureOf(evidenceItem.sourceId(), evidenceItem.claim(), evidenceItem.quoteOrParaphrase());
        Optional<EvidenceItemEntity> existingOpt = repository.findBySignature(signature);
        if (existingOpt.isPresent()) {
            return deserialize(existingOpt.get());
        }

        String evidenceId = StringUtils.hasText(evidenceItem.evidenceId())
                ? evidenceItem.evidenceId() : UUID.randomUUID().toString();
        EvidenceItemEntity entity = new EvidenceItemEntity(
                evidenceId,
                evidenceItem.threadId(),
                evidenceItem.runId(),
                evidenceItem.sourceId(),
                evidenceItem.quoteOrParaphrase(),
                evidenceItem.claim(),
                evidenceItem.dimension(),
                evidenceItem.confidence(),
                evidenceItem.extractedAt() == null ? Instant.now() : evidenceItem.extractedAt(),
                signature
        );
        repository.save(entity);
        return deserialize(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceItem> listByThread(String threadId) {
        if (threadId == null) {
            return List.of();
        }
        return repository.findByThreadId(threadId).stream()
                .map(this::deserialize)
                .sorted(Comparator.comparing(EvidenceItem::extractedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceItem> listByRun(String runId) {
        if (runId == null) {
            return List.of();
        }
        return repository.findByRunId(runId).stream()
                .map(this::deserialize)
                .sorted(Comparator.comparing(EvidenceItem::extractedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceItem> listBySourceId(String sourceId) {
        if (sourceId == null) {
            return List.of();
        }
        return repository.findBySourceId(sourceId).stream()
                .map(this::deserialize)
                .sorted(Comparator.comparing(EvidenceItem::extractedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceItem> listByDimension(String dimension) {
        if (dimension == null) {
            return List.of();
        }
        // InMemory used case-insensitive normalize for dimension lookup
        String normalizedDim = normalize(dimension);
        return repository.findAll().stream() // Using small database, filter memory-side or query
                .filter(entity -> normalizedDim.equals(normalize(entity.getDimension())))
                .map(this::deserialize)
                .sorted(Comparator.comparing(EvidenceItem::extractedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceItem> findById(String evidenceId) {
        if (evidenceId == null) {
            return Optional.empty();
        }
        return repository.findById(evidenceId).map(this::deserialize);
    }

    private EvidenceItem deserialize(EvidenceItemEntity entity) {
        return new EvidenceItem(
                entity.getEvidenceId(),
                entity.getThreadId(),
                entity.getRunId(),
                entity.getSourceId(),
                entity.getQuoteOrParaphrase(),
                entity.getClaim(),
                entity.getDimension(),
                entity.getConfidence(),
                entity.getExtractedAt()
        );
    }

    private static String signatureOf(String sourceId, String claim, String quote) {
        return normalize(sourceId) + "|" + normalize(claim) + "|" + normalize(quote);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
