package org.wrj.haifa.ai.deerflow.model;

import org.springframework.ai.chat.messages.AssistantMessage;
import java.util.Map;

public interface SpringAiProtocolStateCodec {
    boolean supports(String providerAdapter);
    ModelProtocolState capture(AssistantMessage message);
    Map<String, Object> restore(ModelProtocolState state);
}
