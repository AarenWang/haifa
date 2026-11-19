package org.wrj.haifa.ai.deerflow.model;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleThoughtSignatureCodecTest {

    @Test
    public void testCodecSerializationDeserialization() {
        GoogleThoughtSignatureCodec codec = new GoogleThoughtSignatureCodec();

        // 1. Mock thought signatures
        byte[] sig1 = "mock-signature-part-1".getBytes(StandardCharsets.UTF_8);
        byte[] sig2 = "mock-signature-part-2".getBytes(StandardCharsets.UTF_8);
        List<byte[]> expectedSignatures = List.of(sig1, sig2);

        Map<String, Object> props = new HashMap<>();
        props.put("thoughtSignatures", expectedSignatures);
        AssistantMessage msg = AssistantMessage.builder().content("Answer").properties(props).build();

        // 2. Capture
        ModelProtocolState captured = codec.capture(msg);
        assertEquals("google-genai", captured.adapter());
        assertEquals(1, captured.schemaVersion());
        assertTrue(captured.messageExtensions().containsKey("thoughtSignatures"));

        // 3. Restore
        Map<String, Object> properties = codec.restore(captured);
        assertTrue(properties.containsKey("thoughtSignatures"));
        
        List<?> restoredList = (List<?>) properties.get("thoughtSignatures");
        assertEquals(2, restoredList.size());
        
        byte[] restoredSig1 = (byte[]) restoredList.get(0);
        byte[] restoredSig2 = (byte[]) restoredList.get(1);

        assertArrayEquals(sig1, restoredSig1);
        assertArrayEquals(sig2, restoredSig2);
    }

    @Test
    public void testAccumulatorMerging() {
        // Prepare two ModelProtocolStates
        byte[] sig1 = "sig1".getBytes(StandardCharsets.UTF_8);
        byte[] sig2 = "sig2".getBytes(StandardCharsets.UTF_8);
        GoogleThoughtSignatureCodec codec = new GoogleThoughtSignatureCodec();

        Map<String, Object> props1 = new HashMap<>();
        props1.put("thoughtSignatures", List.of(sig1));
        AssistantMessage msg1 = AssistantMessage.builder().content("Chunk 1").properties(props1).build();

        Map<String, Object> props2 = new HashMap<>();
        props2.put("thoughtSignatures", List.of(sig2));
        AssistantMessage msg2 = AssistantMessage.builder().content("Chunk 2").properties(props2).build();

        ModelProtocolState state1 = codec.capture(msg1);
        ModelProtocolState state2 = codec.capture(msg2);

        ModelResponse resp1 = new ModelResponse("Chunk 1", List.of(), List.of(), null, Map.of(), state1);
        ModelResponse resp2 = new ModelResponse("Chunk 2", List.of(), List.of(), "stop", Map.of(), state2);

        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();
        accumulator.accumulate(resp1);
        accumulator.accumulate(resp2);

        ModelResponse finalResponse = accumulator.toResponse();
        assertEquals("Chunk 1Chunk 2", finalResponse.content());
        assertEquals("stop", finalResponse.finishReason());
        
        ModelProtocolState mergedState = finalResponse.protocolState();
        assertEquals("google-genai", mergedState.adapter());

        Map<String, Object> restoredProperties = codec.restore(mergedState);
        List<?> restoredList = (List<?>) restoredProperties.get("thoughtSignatures");
        assertEquals(2, restoredList.size());
        assertArrayEquals(sig1, (byte[]) restoredList.get(0));
        assertArrayEquals(sig2, (byte[]) restoredList.get(1));
    }

    @Test
    void rejectsMalformedBase64DuringRestore() {
        GoogleThoughtSignatureCodec codec = new GoogleThoughtSignatureCodec();
        ModelProtocolState state = new ModelProtocolState(
                1, "google-genai", Map.of("thoughtSignatures", List.of(Map.of(
                        "encoding", "base64", "value", "%%%"))), List.of());

        assertThrows(ModelProtocolStateException.class, () -> codec.restore(state));
    }
}
