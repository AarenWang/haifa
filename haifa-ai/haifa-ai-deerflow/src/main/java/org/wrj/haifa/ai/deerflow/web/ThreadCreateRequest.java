package org.wrj.haifa.ai.deerflow.web;

import java.util.Map;

public record ThreadCreateRequest(String threadId, String title, Map<String, Object> metadata) {
}
