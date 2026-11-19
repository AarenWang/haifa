package org.wrj.haifa.ai.deerflow.memory;

import java.time.Instant;

public record AgentPersonaRecord(
        String id,
        String userId,
        String agentId,
        String name,
        String description,
        String soul,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
