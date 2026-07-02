package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agent_graph_checkpoints", indexes = {
        @Index(name = "idx_graph_checkpoint_run_id", columnList = "run_id"),
        @Index(name = "idx_graph_checkpoint_thread_id", columnList = "thread_id"),
        @Index(name = "idx_graph_checkpoint_checkpoint_id", columnList = "checkpoint_id")
})
public class AgentGraphCheckpointEntity {

    @Id
    @Column(name = "record_id", length = 64, nullable = false)
    private String recordId;

    @Column(name = "checkpoint_id", length = 128, nullable = false)
    private String checkpointId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "graph_name", length = 128)
    private String graphName;

    @Column(name = "node_id", length = 128)
    private String nodeId;

    @Column(name = "next_node_id", length = 128)
    private String nextNodeId;

    @Column(name = "state_summary_json", length = 4000)
    private String stateSummaryJson;

    @Column(name = "full_state_json", length = 1000000)
    private String fullStateJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    public void setCheckpointId(String checkpointId) {
        this.checkpointId = checkpointId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getStateSummaryJson() {
        return stateSummaryJson;
    }

    public void setStateSummaryJson(String stateSummaryJson) {
        this.stateSummaryJson = stateSummaryJson;
    }

    public String getFullStateJson() {
        return fullStateJson;
    }

    public void setFullStateJson(String fullStateJson) {
        this.fullStateJson = fullStateJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
