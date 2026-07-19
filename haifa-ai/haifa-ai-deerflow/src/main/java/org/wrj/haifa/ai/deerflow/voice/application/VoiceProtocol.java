package org.wrj.haifa.ai.deerflow.voice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;

import java.nio.ByteBuffer;
import java.util.Map;

public class VoiceProtocol {

    public static final int HEADER_SIZE = 16;
    public static final short PROTOCOL_VERSION = 1;
    public static final byte DIRECTION_INPUT = 1;
    public static final byte DIRECTION_OUTPUT = 2;
    public static final byte CODEC_PCM_S16LE = 1;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public record ControlMessage(
            String type,
            String voiceSessionId,
            String turnId,
            String threadId,
            String text,
            String runId,
            String reason,
            String code,
            String message,
            Map<String, Object> payload
    ) {
        public static ControlMessage of(String type, String turnId, String text) {
            return new ControlMessage(type, null, turnId, null, text, null, null, null, null, Map.of());
        }

        public static ControlMessage error(String code, String message) {
            return new ControlMessage("error", null, null, null, null, null, null, code, message, Map.of());
        }
    }

    public record BinaryHeader(
            short version,
            byte direction,
            byte codec,
            long sequence,
            int headerLength
    ) {}

    public static String encodeControlJson(ControlMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to encode control message", e);
        }
    }

    public static ControlMessage decodeControlJson(String json) {
        try {
            return objectMapper.readValue(json, ControlMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to decode voice control message", e);
        }
    }

    public static byte[] encodeBinaryAudioFrame(AudioChunk chunk) {
        if (chunk == null) throw new IllegalArgumentException("Audio chunk must not be null");
        byte[] payload = chunk.data();
        if (payload.length == 0 || (payload.length & 1) != 0) {
            throw new IllegalArgumentException("PCM audio chunk must contain a non-empty even number of bytes");
        }
        if (chunk.sequence() <= 0) {
            throw new IllegalArgumentException("Audio chunk sequence must be positive");
        }
        if (!"pcm_s16le".equalsIgnoreCase(chunk.format().codec())) {
            throw new IllegalArgumentException("Only pcm_s16le output is supported by voice protocol v1");
        }
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payload.length);
        buffer.putShort(PROTOCOL_VERSION);
        buffer.put(DIRECTION_OUTPUT);
        buffer.put(CODEC_PCM_S16LE);
        buffer.putLong(chunk.sequence());
        buffer.putInt(0); // header length
        buffer.put(payload);
        return buffer.array();
    }

    public static AudioChunk decodeBinaryAudioFrame(byte[] frame, AudioFormat format) {
        if (frame.length < HEADER_SIZE) {
            throw new IllegalArgumentException("Invalid binary frame size: " + frame.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(frame);
        short version = buffer.getShort();
        byte direction = buffer.get();
        byte codec = buffer.get();
        long sequence = buffer.getLong();
        int headerLen = buffer.getInt();
        if (version != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported voice protocol version: " + version);
        }
        if (direction != DIRECTION_INPUT) {
            throw new IllegalArgumentException("Expected input audio frame direction");
        }
        if (codec != CODEC_PCM_S16LE) {
            throw new IllegalArgumentException("Unsupported input audio codec: " + codec);
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("Audio frame sequence must be positive");
        }
        if (headerLen < 0 || HEADER_SIZE + headerLen > frame.length) {
            throw new IllegalArgumentException("Invalid binary envelope header length: " + headerLen);
        }

        int payloadOffset = HEADER_SIZE + headerLen;
        int payloadLen = frame.length - payloadOffset;
        if (payloadLen == 0 || (payloadLen & 1) != 0) {
            throw new IllegalArgumentException("PCM payload must contain a non-empty even number of bytes");
        }
        byte[] payload = new byte[payloadLen];
        System.arraycopy(frame, payloadOffset, payload, 0, payloadLen);

        return new AudioChunk(sequence, payload, format);
    }
}
