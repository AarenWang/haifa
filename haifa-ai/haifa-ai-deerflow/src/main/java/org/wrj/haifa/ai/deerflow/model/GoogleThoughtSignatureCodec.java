package org.wrj.haifa.ai.deerflow.model;

import org.springframework.ai.chat.messages.AssistantMessage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class GoogleThoughtSignatureCodec implements SpringAiProtocolStateCodec {

    private static final String SIGNATURES_KEY = "thoughtSignatures";
    private static final String ENCODING_KEY = "encoding";
    private static final String VALUE_KEY = "value";
    private static final String BASE64 = "base64";

    @Override
    public boolean supports(String providerAdapter) {
        return "google-genai".equalsIgnoreCase(providerAdapter) || "google".equalsIgnoreCase(providerAdapter);
    }

    @Override
    public ModelProtocolState capture(AssistantMessage message) {
        if (message == null || message.getMetadata() == null) {
            return ModelProtocolState.empty();
        }
        Object thoughtsObj = message.getMetadata().get(SIGNATURES_KEY);
        if (thoughtsObj instanceof List<?> list) {
            List<Map<String, String>> encodedSignatures = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof byte[] bytes) {
                    encodedSignatures.add(Map.of(
                            ENCODING_KEY, BASE64,
                            VALUE_KEY, Base64.getEncoder().encodeToString(bytes)));
                }
                else {
                    throw new ModelProtocolStateException(
                            "Google thought signatures must be byte arrays");
                }
            }
            if (!encodedSignatures.isEmpty()) {
                return new ModelProtocolState(
                        ModelProtocolState.CURRENT_SCHEMA_VERSION,
                        "google-genai",
                        Map.of(SIGNATURES_KEY, encodedSignatures),
                        List.of()
                );
            }
        }
        else if (thoughtsObj != null) {
            throw new ModelProtocolStateException("Google thought signatures must be a list");
        }
        return ModelProtocolState.empty();
    }

    @Override
    public Map<String, Object> restore(ModelProtocolState state) {
        if (state == null || !supports(state.adapter())) {
            return Map.of();
        }
        Object listObj = state.messageExtensions().get(SIGNATURES_KEY);
        if (listObj instanceof List<?> list) {
            List<byte[]> bytesList = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> envelope)
                        || !BASE64.equals(envelope.get(ENCODING_KEY))
                        || !(envelope.get(VALUE_KEY) instanceof String encoded)) {
                    throw new ModelProtocolStateException(
                            "Google thought signature must use a Base64 envelope");
                }
                try {
                    bytesList.add(Base64.getDecoder().decode(encoded));
                }
                catch (IllegalArgumentException ex) {
                    throw new ModelProtocolStateException(
                            "Invalid Base64 Google thought signature", ex);
                }
            }
            if (!bytesList.isEmpty()) {
                return Map.of(SIGNATURES_KEY, bytesList);
            }
        }
        else if (listObj != null) {
            throw new ModelProtocolStateException("Google thought signatures must be a list");
        }
        return Map.of();
    }
}
