package org.wrj.haifa.ai.deerflow.web;

import java.util.List;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;

public record MessageListResponse(List<MessageRecord> messages) {
    public MessageListResponse {
        if (messages != null) {
            messages = messages.stream()
                    .map(m -> {
                        if (m.metadata() != null && m.metadata().containsKey("protocolState")) {
                            var cleanMetadata = new java.util.LinkedHashMap<>(m.metadata());
                            cleanMetadata.remove("protocolState");
                            return new MessageRecord(m.messageId(), m.threadId(), m.runId(), m.role(), m.content(), cleanMetadata, m.createdAt());
                        }
                        return m;
                    })
                    .toList();
        }
    }
}
