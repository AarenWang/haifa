package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.AgentGraphCheckpointRecord;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentGraphCheckpointEntity;

@Component
public class AgentGraphCheckpointMapper {

    private final JsonMapper jsonMapper;

    public AgentGraphCheckpointMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AgentGraphCheckpointEntity toEntity(AgentGraphCheckpointRecord record) {
        AgentGraphCheckpointEntity entity = new AgentGraphCheckpointEntity();
        entity.setRecordId(record.recordId());
        entity.setCheckpointId(record.checkpointId());
        entity.setRunId(record.runId());
        entity.setThreadId(record.threadId());
        entity.setGraphName(record.graphName());
        entity.setNodeId(record.nodeId());
        entity.setNextNodeId(record.nextNodeId());
        entity.setStateSummaryJson(jsonMapper.toJson(record.stateSummary()));
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    public AgentGraphCheckpointRecord toRecord(AgentGraphCheckpointEntity entity) {
        return new AgentGraphCheckpointRecord(
                entity.getRecordId(),
                entity.getCheckpointId(),
                entity.getRunId(),
                entity.getThreadId(),
                entity.getGraphName(),
                entity.getNodeId(),
                entity.getNextNodeId(),
                jsonMapper.fromJson(entity.getStateSummaryJson()),
                entity.getCreatedAt()
        );
    }
}
