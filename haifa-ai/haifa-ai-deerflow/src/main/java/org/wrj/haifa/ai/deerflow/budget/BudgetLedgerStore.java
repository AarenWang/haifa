package org.wrj.haifa.ai.deerflow.budget;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BudgetLedgerStore {

    private final BudgetLedgerRepository repository;

    public BudgetLedgerStore(BudgetLedgerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BudgetLedger init(String runId, Long maxElapsedMs, Integer maxModelCalls, Integer maxToolCalls, Integer maxSearchQueries, Integer maxFetchedSources, Integer maxReplans, Integer maxSubagents) {
        BudgetLedger ledger = new BudgetLedger();
        ledger.setRunId(runId);
        ledger.setMaxElapsedMs(maxElapsedMs != null ? maxElapsedMs : 1800000L); // default 30 mins
        ledger.setUsedElapsedMs(0L);
        ledger.setMaxModelCalls(maxModelCalls != null ? maxModelCalls : 20);
        ledger.setUsedModelCalls(0);
        ledger.setMaxToolCalls(maxToolCalls != null ? maxToolCalls : 80);
        ledger.setUsedToolCalls(0);
        ledger.setMaxSearchQueries(maxSearchQueries != null ? maxSearchQueries : 30);
        ledger.setUsedSearchQueries(0);
        ledger.setMaxFetchedSources(maxFetchedSources != null ? maxFetchedSources : 20);
        ledger.setUsedFetchedSources(0);
        ledger.setMaxReplans(maxReplans != null ? maxReplans : 5);
        ledger.setUsedReplans(0);
        ledger.setMaxSubagents(maxSubagents != null ? maxSubagents : 5);
        ledger.setUsedSubagents(0);
        ledger.setStopReason(null);
        return repository.save(ledger);
    }

    @Transactional
    public void incrementModelCalls(String runId) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedModelCalls(l.getUsedModelCalls() + 1);
            repository.save(l);
        });
    }

    @Transactional
    public void incrementToolCalls(String runId) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedToolCalls(l.getUsedToolCalls() + 1);
            repository.save(l);
        });
    }

    @Transactional
    public void incrementSearchQueries(String runId) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedSearchQueries(l.getUsedSearchQueries() + 1);
            repository.save(l);
        });
    }

    @Transactional
    public void incrementFetchedSources(String runId) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedFetchedSources(l.getUsedFetchedSources() + 1);
            repository.save(l);
        });
    }

    @Transactional
    public void incrementReplans(String runId) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedReplans(l.getUsedReplans() + 1);
            repository.save(l);
        });
    }

    @Transactional
    public void incrementSubagents(String runId) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedSubagents(l.getUsedSubagents() + 1);
            repository.save(l);
        });
    }

    @Transactional
    public void updateElapsed(String runId, long elapsedMs) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setUsedElapsedMs(elapsedMs);
            repository.save(l);
        });
    }

    @Transactional
    public void updateStopReason(String runId, String stopReason) {
        repository.findByRunId(runId).ifPresent(l -> {
            l.setStopReason(stopReason);
            repository.save(l);
        });
    }

    @Transactional(readOnly = true)
    public Optional<BudgetLedger> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }
}
