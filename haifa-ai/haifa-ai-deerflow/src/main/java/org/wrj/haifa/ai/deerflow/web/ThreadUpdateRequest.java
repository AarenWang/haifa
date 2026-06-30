package org.wrj.haifa.ai.deerflow.web;

import java.util.Map;
import org.wrj.haifa.ai.deerflow.thread.ThreadStatus;

public record ThreadUpdateRequest(String title, ThreadStatus status, Map<String, Object> metadata) {
}
