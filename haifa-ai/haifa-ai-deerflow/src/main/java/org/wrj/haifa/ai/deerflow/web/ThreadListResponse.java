package org.wrj.haifa.ai.deerflow.web;

import java.util.List;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;

public record ThreadListResponse(List<ThreadRecord> threads) {
}
