package org.wrj.haifa.ai.deerflow.voice;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.voice.application.VoiceProtocol;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;

import static org.junit.jupiter.api.Assertions.*;

public class VoiceProtocolCodecTest {

    @Test
    public void testControlMessageJsonEncodingAndDecoding() {
        VoiceProtocol.ControlMessage msg = VoiceProtocol.ControlMessage.of("turn.start", "turn_123", "hello");
        String json = VoiceProtocol.encodeControlJson(msg);
        assertNotNull(json);
        assertTrue(json.contains("turn.start"));

        VoiceProtocol.ControlMessage decoded = VoiceProtocol.decodeControlJson(json);
        assertEquals("turn.start", decoded.type());
        assertEquals("turn_123", decoded.turnId());
    }

    @Test
    public void testBinaryAudioFrameEncodingAndDecoding() {
        byte[] pcm = new byte[]{0x01, 0x02, 0x03, 0x04};
        AudioChunk chunk = new AudioChunk(42L, pcm, AudioFormat.DEFAULT_INPUT);

        byte[] frame = VoiceProtocol.encodeBinaryAudioFrame(chunk);
        assertEquals(VoiceProtocol.HEADER_SIZE + pcm.length, frame.length);
        frame[2] = VoiceProtocol.DIRECTION_INPUT;

        AudioChunk decoded = VoiceProtocol.decodeBinaryAudioFrame(frame, AudioFormat.DEFAULT_INPUT);
        assertEquals(42L, decoded.sequence());
        assertArrayEquals(pcm, decoded.data());
    }

    @Test
    public void testRejectsWrongDirectionAndVersion() {
        AudioChunk chunk = new AudioChunk(1L, new byte[]{0x01, 0x02}, AudioFormat.DEFAULT_OUTPUT);
        byte[] outputFrame = VoiceProtocol.encodeBinaryAudioFrame(chunk);
        assertThrows(IllegalArgumentException.class,
                () -> VoiceProtocol.decodeBinaryAudioFrame(outputFrame, AudioFormat.DEFAULT_INPUT));

        outputFrame[2] = VoiceProtocol.DIRECTION_INPUT;
        outputFrame[1] = 2;
        assertThrows(IllegalArgumentException.class,
                () -> VoiceProtocol.decodeBinaryAudioFrame(outputFrame, AudioFormat.DEFAULT_INPUT));
    }

    @Test
    public void testRejectsInvalidOutputPcmEnvelope() {
        assertThrows(IllegalArgumentException.class, () -> VoiceProtocol.encodeBinaryAudioFrame(
                new AudioChunk(0L, new byte[]{0x01, 0x02}, AudioFormat.DEFAULT_OUTPUT)));
        assertThrows(IllegalArgumentException.class, () -> VoiceProtocol.encodeBinaryAudioFrame(
                new AudioChunk(1L, new byte[]{0x01}, AudioFormat.DEFAULT_OUTPUT)));
    }
}
