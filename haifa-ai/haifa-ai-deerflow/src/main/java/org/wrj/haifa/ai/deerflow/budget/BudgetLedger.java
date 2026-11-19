package org.wrj.haifa.ai.deerflow.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deerflow_budget_ledgers")
public class BudgetLedger {

    @Id
    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "max_elapsed_ms")
    private Long maxElapsedMs;

    @Column(name = "used_elapsed_ms")
    private Long usedElapsedMs;

    @Column(name = "max_model_calls")
    private Integer maxModelCalls;

    @Column(name = "used_model_calls")
    private Integer usedModelCalls;

    @Column(name = "max_tool_calls")
    private Integer maxToolCalls;

    @Column(name = "used_tool_calls")
    private Integer usedToolCalls;

    @Column(name = "max_search_queries")
    private Integer maxSearchQueries;

    @Column(name = "used_search_queries")
    private Integer usedSearchQueries;

    @Column(name = "max_fetched_sources")
    private Integer maxFetchedSources;

    @Column(name = "used_fetched_sources")
    private Integer usedFetchedSources;

    @Column(name = "max_replans")
    private Integer maxReplans;

    @Column(name = "used_replans")
    private Integer usedReplans;

    @Column(name = "max_subagents")
    private Integer maxSubagents;

    @Column(name = "used_subagents")
    private Integer usedSubagents;

    @Column(name = "stop_reason", length = 256)
    private String stopReason;

    public BudgetLedger() {
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Long getMaxElapsedMs() {
        return maxElapsedMs;
    }

    public void setMaxElapsedMs(Long maxElapsedMs) {
        this.maxElapsedMs = maxElapsedMs;
    }

    public Long getUsedElapsedMs() {
        return usedElapsedMs;
    }

    public void setUsedElapsedMs(Long usedElapsedMs) {
        this.usedElapsedMs = usedElapsedMs;
    }

    public Integer getMaxModelCalls() {
        return maxModelCalls;
    }

    public void setMaxModelCalls(Integer maxModelCalls) {
        this.maxModelCalls = maxModelCalls;
    }

    public Integer getUsedModelCalls() {
        return usedModelCalls;
    }

    public void setUsedModelCalls(Integer usedModelCalls) {
        this.usedModelCalls = usedModelCalls;
    }

    public Integer getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(Integer maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public Integer getUsedToolCalls() {
        return usedToolCalls;
    }

    public void setUsedToolCalls(Integer usedToolCalls) {
        this.usedToolCalls = usedToolCalls;
    }

    public Integer getMaxSearchQueries() {
        return maxSearchQueries;
    }

    public void setMaxSearchQueries(Integer maxSearchQueries) {
        this.maxSearchQueries = maxSearchQueries;
    }

    public Integer getUsedSearchQueries() {
        return usedSearchQueries;
    }

    public void setUsedSearchQueries(Integer usedSearchQueries) {
        this.usedSearchQueries = usedSearchQueries;
    }

    public Integer getMaxFetchedSources() {
        return maxFetchedSources;
    }

    public void setMaxFetchedSources(Integer maxFetchedSources) {
        this.maxFetchedSources = maxFetchedSources;
    }

    public Integer getUsedFetchedSources() {
        return usedFetchedSources;
    }

    public void setUsedFetchedSources(Integer usedFetchedSources) {
        this.usedFetchedSources = usedFetchedSources;
    }

    public Integer getMaxReplans() {
        return maxReplans;
    }

    public void setMaxReplans(Integer maxReplans) {
        this.maxReplans = maxReplans;
    }

    public Integer getUsedReplans() {
        return usedReplans;
    }

    public void setUsedReplans(Integer usedReplans) {
        this.usedReplans = usedReplans;
    }

    public Integer getMaxSubagents() {
        return maxSubagents;
    }

    public void setMaxSubagents(Integer maxSubagents) {
        this.maxSubagents = maxSubagents;
    }

    public Integer getUsedSubagents() {
        return usedSubagents;
    }

    public void setUsedSubagents(Integer usedSubagents) {
        this.usedSubagents = usedSubagents;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}
