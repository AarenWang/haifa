package org.wrj.haifa.ai.deerflow.web;

import java.util.List;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;

public record MessageListResponse(List<MessageRecord> messages) {
}
