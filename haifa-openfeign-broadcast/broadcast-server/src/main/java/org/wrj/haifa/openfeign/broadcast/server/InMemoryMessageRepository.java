package org.wrj.haifa.openfeign.broadcast.server;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.wrj.haifa.openfeign.broadcast.shared.MessageRecord;

/**
 * Thread-safe, in-memory storage of broadcast messages per instance.
 */
@Component
class InMemoryMessageRepository {

    private final CopyOnWriteArrayList<MessageRecord> records = new CopyOnWriteArrayList<>();

    List<MessageRecord> list() {
        return List.copyOf(records);
    }

    int append(String message, String origin, Instant receivedAt) {
        records.add(new MessageRecord(message, origin, receivedAt));
        return records.size();
    }
}
